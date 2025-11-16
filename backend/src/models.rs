use serde::{Deserialize, Serialize};
use serde_repr::{Deserialize_repr, Serialize_repr};

// Settings (read-only, loaded on startup)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Settings {
    pub ap_name: String,
    pub ap_password: String,
    pub admin_users: Vec<String>,
}

// User
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub username: String,
    pub password_hash: String,
    pub salt: String,
}

#[derive(Debug, Deserialize)]
pub struct RegisterRequest {
    pub username: String,
    pub password: String,
}

#[derive(Debug, Deserialize)]
pub struct LoginRequest {
    pub username: String,
    pub password: String,
}

#[derive(Debug, Serialize)]
pub struct LoginResponse {
    pub token: String,
    pub username: String,
    pub is_admin: bool,
}

// Hold Type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize_repr, Deserialize_repr)]
#[repr(u8)]
pub enum HoldType {
    Start = 0,
    Foot = 1,
    Normal = 2,
    End = 3,
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub struct Hold(pub u16, pub HoldType);

// Problem
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BaseProblem {
    pub id: u32,
    pub name: String,
    pub description: Option<String>,
    pub author: String,
    pub grade: u8,
    pub sector_id: u16,
    pub updated_at: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DiskProblem {
    #[serde(flatten)]
    pub base: BaseProblem,
    pub hold_sequence: Vec<Hold>,
    pub grades: Vec<Grade>,
}

#[derive(Debug, Serialize)]
pub struct APIProblemSummary {
    #[serde(flatten)]
    pub base: BaseProblem,
    pub average_grade: Option<f32>,
    pub average_stars: Option<f32>,
}

#[derive(Debug, Serialize)]
pub struct APIProblemDetail {
    #[serde(flatten)]
    pub base: DiskProblem,
    pub average_grade: Option<f32>,
    pub average_stars: Option<f32>,
}

#[derive(Debug, Serialize)]
pub struct ProblemList {
    pub problems: Vec<APIProblemSummary>,
    pub total: u32,
    pub page: u32,
    pub per_page: u32,
}

#[derive(Debug, Deserialize)]
pub struct CreateProblemRequest {
    pub name: Option<String>,
    pub description: Option<String>,
    pub grade: u8,
    pub sector_id: u16,
    pub hold_sequence: Vec<Hold>,
}

#[derive(Debug, Deserialize)]
pub struct UpdateProblemRequest {
    pub name: Option<String>,
    pub description: Option<String>,
    pub grade: Option<u8>,
    pub hold_sequence: Option<Vec<Hold>>,
}

// Grade
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Grade {
    pub username: String,
    pub grade: u8,
    pub stars: u8,
    pub created_at: String,
}

#[derive(Debug, Deserialize)]
pub struct SubmitGradeRequest {
    pub grade: u8,
    pub stars: u8,
}

// Sector
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DiskSectorMetadata {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image_filename: Option<String>,
    pub holds: Vec<[u16; 4]>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub id: Option<u16>,
    #[serde(skip, default)]
    pub image_width: u32,
    #[serde(skip, default)]
    pub image_height: u32,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub display_name: Option<String>,
    #[serde(skip, default)]
    pub folder_name: String,
}

#[derive(Debug, Clone, Serialize)]
pub struct APISectorSummary {
    pub id: u16,
    pub name: String,
}

#[derive(Debug, Serialize)]
pub struct APISectorDetail {
    pub id: u16,
    pub name: String,
    pub holds: Vec<[u16; 4]>,
    pub image_width: u32,
    pub image_height: u32,
}

// Error
#[derive(Debug, Serialize)]
pub struct ErrorResponse {
    pub error: String,
    pub code: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub timeout: Option<u64>,
}

impl DiskProblem {
    pub fn calculate_averages(&self) -> (Option<f32>, Option<f32>) {
        if self.grades.is_empty() {
            return (None, None);
        }

        let avg_grade =
            self.grades.iter().map(|g| g.grade as f32).sum::<f32>() / self.grades.len() as f32;
        let avg_stars =
            self.grades.iter().map(|g| g.stars as f32).sum::<f32>() / self.grades.len() as f32;

        (Some(avg_grade), Some(avg_stars))
    }

    pub fn to_summary(self) -> APIProblemSummary {
        let (avg_grade, avg_stars) = self.calculate_averages();
        APIProblemSummary {
            base: self.base,
            average_grade: avg_grade,
            average_stars: avg_stars,
        }
    }

    pub fn to_detail(self) -> APIProblemDetail {
        let (avg_grade, avg_stars) = self.calculate_averages();
        APIProblemDetail {
            base: self,
            average_grade: avg_grade,
            average_stars: avg_stars,
        }
    }
}
