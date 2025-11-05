use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use tokio::sync::RwLock;

use crate::auth::SessionManager;
use crate::models::{Problem, SectorMetadata, SectorSummary, Settings, User};
use crate::rate_limit::RateLimiter;
use std::fs::File;
use std::io::BufReader;

pub struct AppState {
    pub settings: Settings,
    pub users: Arc<RwLock<Vec<User>>>,
    pub problems: Arc<RwLock<Vec<Problem>>>,
    pub sessions: Arc<RwLock<SessionManager>>,
    pub next_problem_id: Arc<RwLock<u32>>,
    pub dirty: Arc<RwLock<bool>>,
    pub rate_limiter: Arc<RwLock<RateLimiter>>,
    data_path: PathBuf,
    pub sectors_path: PathBuf,
    pub sectors: Vec<SectorSummary>,
    pub sector_metadata: HashMap<u16, SectorMetadata>,
}

impl AppState {
    pub async fn new(
        data_path: PathBuf,
        sectors_path: PathBuf,
    ) -> Result<Self, Box<dyn std::error::Error>> {
        let settings_path = data_path.join("settings.json");
        let default_settings = Settings {
            ap_name: "AscendoTrainBoard".to_string(),
            ap_password: "plezaj-gor".to_string(),
            admin_users: vec!["admin".to_string()],
        };
        let settings = if settings_path.exists() {
            match tokio::fs::read_to_string(&settings_path).await {
                Ok(data) => serde_json::from_str(&data).unwrap_or_else(|_| default_settings),
                Err(e) => {
                    eprintln!("Error reading settings.json: {e}");
                    default_settings
                }
            }
        } else {
            default_settings
        };

        let users_path = data_path.join("users.json");
        let users = if users_path.exists() {
            match tokio::fs::read_to_string(&users_path).await {
                Ok(data) => serde_json::from_str(&data).unwrap_or_else(|_| Vec::new()),
                Err(e) => {
                    eprintln!("Error reading users.json: {e}");
                    Vec::new()
                }
            }
        } else {
            Vec::new()
        };

        let problems_path = data_path.join("problems.json");
        let problems: Vec<Problem> = if problems_path.exists() {
            match tokio::fs::read_to_string(&problems_path).await {
                Ok(data) => serde_json::from_str(&data).unwrap_or_else(|_| Vec::new()),
                Err(_) => Vec::new(),
            }
        } else {
            Vec::new()
        };

        let next_id = problems.iter().map(|p| p.id).max().unwrap_or(0) + 1;

        let (sectors, sector_metadata) = Self::load_sectors(&sectors_path)
            .await
            .unwrap_or_else(|_| (Vec::new(), HashMap::new()));

        Ok(Self {
            settings,
            users: Arc::new(RwLock::new(users)),
            problems: Arc::new(RwLock::new(problems)),
            sessions: Arc::new(RwLock::new(SessionManager::new())),
            next_problem_id: Arc::new(RwLock::new(next_id)),
            dirty: Arc::new(RwLock::new(false)),
            rate_limiter: Arc::new(RwLock::new(RateLimiter::new())),
            data_path,
            sectors_path,
            sectors,
            sector_metadata,
        })
    }

    async fn find_image_file(path: &Path) -> Option<String> {
        let mut dir_entries = tokio::fs::read_dir(path).await.ok()?;
        while let Ok(Some(entry)) = dir_entries.next_entry().await {
            let filename = entry.file_name().to_str()?.to_string();
            let lower = filename.to_lowercase();
            if lower.ends_with(".jpg") || lower.ends_with(".jpeg") || lower.ends_with(".png") {
                return Some(filename);
            }
        }
        None
    }

    fn read_image_dimensions(path: &Path) -> Option<(u32, u32)> {
        let file = File::open(path).ok()?;
        let size = imagesize::reader_size(BufReader::new(file)).ok()?;
        Some((size.width as u32, size.height as u32))
    }

    async fn load_sectors(
        sectors_path: &PathBuf,
    ) -> Result<(Vec<SectorSummary>, HashMap<u16, SectorMetadata>), Box<dyn std::error::Error>>
    {
        let mut sector_data = Vec::new();
        let mut max_id = 0u16;

        let mut entries = tokio::fs::read_dir(sectors_path).await?;

        while let Ok(Some(entry)) = entries.next_entry().await {
            let path = entry.path();

            if !path.is_dir() {
                continue;
            }

            let Some(folder_name) = path.file_name().and_then(|n| n.to_str()).map(String::from)
            else {
                continue;
            };

            let metadata_path = path.join("metadata.json");
            if !metadata_path.exists() {
                continue;
            }

            let Ok(data) = tokio::fs::read_to_string(&metadata_path).await else {
                continue;
            };

            let Ok(mut metadata) = serde_json::from_str::<SectorMetadata>(&data) else {
                continue;
            };

            // Auto-detect image file if missing
            let image_filename = if let Some(ref filename) = metadata.image_filename {
                filename.to_string()
            } else {
                match Self::find_image_file(&path).await {
                    Some(filename) => {
                        metadata.image_filename = Some(filename.clone());
                        filename
                    }
                    None => {
                        eprintln!(
                            "No image file found in sector directory: {}",
                            path.display()
                        );
                        continue;
                    }
                }
            };

            // Always read image dimensions from the actual image file
            let image_path = path.join(&image_filename);
            let Some((width, height)) = Self::read_image_dimensions(&image_path) else {
                eprintln!(
                    "Failed to read image dimensions for {}",
                    image_path.display()
                );
                continue;
            };

            metadata.image_width = width;
            metadata.image_height = height;
            metadata.folder_name = folder_name.clone();

            // Track max ID
            if let Some(id) = metadata.id {
                max_id = max_id.max(id);
            }

            sector_data.push((folder_name, metadata));
        }

        sector_data.sort_by(|a, b| a.0.cmp(&b.0));

        let mut sectors = Vec::new();
        let mut id2sector_metadata = HashMap::new();

        // Assign IDs and write back if needed
        for (folder_name, mut metadata) in sector_data {
            let needs_write = if metadata.id.is_none() {
                max_id += 1;
                metadata.id = Some(max_id);
                true
            } else {
                false
            };

            if needs_write {
                let metadata_path = sectors_path.join(&folder_name).join("metadata.json");
                if let Ok(json) = serde_json::to_string_pretty(&metadata) {
                    let _ = std::fs::write(&metadata_path, json);
                }
            }

            let id = metadata.id.unwrap();
            sectors.push(SectorSummary {
                id,
                name: folder_name,
            });
            id2sector_metadata.insert(id, metadata);
        }

        Ok((sectors, id2sector_metadata))
    }

    pub fn mark_dirty(&self) {
        let dirty = self.dirty.clone();
        tokio::spawn(async move {
            *dirty.write().await = true;
        });
    }

    pub async fn save_if_dirty(&self) -> Result<(), Box<dyn std::error::Error>> {
        let mut dirty = self.dirty.write().await;
        if !*dirty {
            return Ok(());
        }

        self.save().await?;
        *dirty = false;
        Ok(())
    }

    pub async fn save(&self) -> Result<(), Box<dyn std::error::Error>> {
        let users = self.users.read().await;
        let users_json = serde_json::to_string(&*users)?;
        tokio::fs::write(self.data_path.join("users.json"), users_json).await?;

        let problems = self.problems.read().await;
        let problems_json = serde_json::to_string(&*problems)?;
        tokio::fs::write(self.data_path.join("problems.json"), problems_json).await?;

        Ok(())
    }

    pub fn is_admin(&self, username: &str) -> bool {
        self.settings.admin_users.contains(&username.to_string())
    }

    pub async fn get_next_problem_id(&self) -> u32 {
        let mut next_id = self.next_problem_id.write().await;
        let id = *next_id;
        *next_id += 1;
        id
    }
}

impl Clone for AppState {
    fn clone(&self) -> Self {
        Self {
            settings: self.settings.clone(),
            users: Arc::clone(&self.users),
            problems: Arc::clone(&self.problems),
            sessions: Arc::clone(&self.sessions),
            next_problem_id: Arc::clone(&self.next_problem_id),
            dirty: Arc::clone(&self.dirty),
            rate_limiter: Arc::clone(&self.rate_limiter),
            data_path: self.data_path.clone(),
            sectors_path: self.sectors_path.clone(),
            sectors: self.sectors.clone(),
            sector_metadata: self.sector_metadata.clone(),
        }
    }
}
