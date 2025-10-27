use std::path::PathBuf;
use std::sync::Arc;
use tokio::sync::RwLock;

use crate::auth::SessionManager;
use crate::models::{Problem, SectorMetadata, SectorSummary, Settings, User};

pub struct AppState {
    pub settings: Settings,
    pub users: Arc<RwLock<Vec<User>>>,
    pub problems: Arc<RwLock<Vec<Problem>>>,
    pub sessions: Arc<RwLock<SessionManager>>,
    pub next_problem_id: Arc<RwLock<u32>>,
    pub dirty: Arc<RwLock<bool>>,
    data_path: PathBuf,
    pub sectors_path: PathBuf,
    pub sectors: Vec<SectorSummary>,
    pub sector_metadata: std::collections::HashMap<String, SectorMetadata>,
}

impl AppState {
    pub async fn new(
        data_path: PathBuf,
        sectors_path: PathBuf,
    ) -> Result<Self, Box<dyn std::error::Error>> {
        let settings_path = data_path.join("settings.json");
        let settings = if settings_path.exists() {
            let data = tokio::fs::read_to_string(&settings_path).await?;
            serde_json::from_str(&data)?
        } else {
            Settings {
                ap_name: "AscendoTrainBoard".to_string(),
                ap_password: "changeme".to_string(),
                admin_users: vec!["admin".to_string()],
            }
        };

        let users_path = data_path.join("users.json");
        let users = if users_path.exists() {
            let data = tokio::fs::read_to_string(&users_path).await?;
            serde_json::from_str(&data)?
        } else {
            Vec::new()
        };

        let problems_path = data_path.join("problems.json");
        let problems: Vec<Problem> = if problems_path.exists() {
            let data = tokio::fs::read_to_string(&problems_path).await?;
            serde_json::from_str(&data)?
        } else {
            Vec::new()
        };

        let next_id = problems.iter().map(|p| p.id).max().unwrap_or(0) + 1;

        let (sectors, sector_metadata) = Self::load_sectors(&sectors_path).await?;

        Ok(Self {
            settings,
            users: Arc::new(RwLock::new(users)),
            problems: Arc::new(RwLock::new(problems)),
            sessions: Arc::new(RwLock::new(SessionManager::new())),
            next_problem_id: Arc::new(RwLock::new(next_id)),
            dirty: Arc::new(RwLock::new(false)),
            data_path,
            sectors_path,
            sectors,
            sector_metadata,
        })
    }

    async fn load_sectors(
        sectors_path: &PathBuf,
    ) -> Result<
        (
            Vec<SectorSummary>,
            std::collections::HashMap<String, SectorMetadata>,
        ),
        Box<dyn std::error::Error>,
    > {
        let mut sectors = Vec::new();
        let mut metadata_map = std::collections::HashMap::new();

        let mut entries = tokio::fs::read_dir(sectors_path).await?;

        while let Ok(Some(entry)) = entries.next_entry().await {
            let path = entry.path();

            if !path.is_dir() {
                continue;
            }

            let folder_name = match path.file_name().and_then(|n| n.to_str()) {
                Some(name) => name.to_string(),
                None => continue,
            };

            let metadata_path = path.join("metadata.json");

            if !metadata_path.exists() {
                continue;
            }

            match tokio::fs::read_to_string(&metadata_path).await {
                Ok(data) => match serde_json::from_str::<SectorMetadata>(&data) {
                    Ok(metadata) => {
                        sectors.push(SectorSummary {
                            name: folder_name.clone(),
                        });
                        metadata_map.insert(folder_name, metadata);
                    }
                    Err(_) => continue,
                },
                Err(_) => continue,
            }
        }

        sectors.sort_by(|a, b| a.name.cmp(&b.name));

        Ok((sectors, metadata_map))
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
        let users_json = serde_json::to_string_pretty(&*users)?;
        tokio::fs::write(self.data_path.join("users.json"), users_json).await?;

        let problems = self.problems.read().await;
        let problems_json = serde_json::to_string_pretty(&*problems)?;
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
            data_path: self.data_path.clone(),
            sectors_path: self.sectors_path.clone(),
            sectors: self.sectors.clone(),
            sector_metadata: self.sector_metadata.clone(),
        }
    }
}
