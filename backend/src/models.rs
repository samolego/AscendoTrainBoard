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

#[derive(Debug, Serialize)]
pub struct UserResponse {
    pub username: String,
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
pub struct Hold(pub u8, pub u8, pub HoldType);

// Problem
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Problem {
    pub id: u32,
    pub name: String,
    pub description: Option<String>,
    pub author: String,
    pub grade: u8,
    pub sector_name: String,
    pub hold_sequence: Vec<Hold>,
    pub grades: Vec<Grade>,
    pub updated_at: String,
}

#[derive(Debug, Serialize)]
pub struct ProblemSummary {
    pub id: u32,
    pub name: String,
    pub description: Option<String>,
    pub author: String,
    pub grade: u8,
    pub sector_name: String,
    pub average_grade: Option<f32>,
    pub average_stars: Option<f32>,
    pub updated_at: String,
}

#[derive(Debug, Serialize)]
pub struct ProblemDetail {
    pub id: u32,
    pub name: String,
    pub description: Option<String>,
    pub author: String,
    pub grade: u8,
    pub sector_name: String,
    pub hold_sequence: Vec<Hold>,
    pub average_grade: Option<f32>,
    pub average_stars: Option<f32>,
    pub updated_at: String,
}

#[derive(Debug, Serialize)]
pub struct ProblemList {
    pub problems: Vec<ProblemSummary>,
    pub total: u32,
    pub page: u32,
    pub per_page: u32,
}

#[derive(Debug, Deserialize)]
pub struct CreateProblemRequest {
    pub name: Option<String>,
    pub description: Option<String>,
    pub grade: u8,
    pub sector_name: String,
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

#[derive(Debug, Serialize)]
pub struct ProblemGrades {
    pub problem_id: u32,
    pub grades: Vec<Grade>,
    pub average_grade: Option<f32>,
    pub average_stars: Option<f32>,
}

#[derive(Debug, Deserialize)]
pub struct SubmitGradeRequest {
    pub grade: u8,
    pub stars: u8,
}

// Sector
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SectorMetadata {
    pub image_filename: String,
    pub holds: Vec<[u16; 4]>,
}

#[derive(Debug, Clone, Serialize)]
pub struct SectorSummary {
    pub name: String,
}

#[derive(Debug, Serialize)]
pub struct Sector {
    pub name: String,
    pub holds: Vec<[u16; 4]>,
}

// Error
#[derive(Debug, Serialize)]
pub struct ErrorResponse {
    pub error: String,
    pub code: String,
}

impl Problem {
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

    pub fn to_summary(&self) -> ProblemSummary {
        let (avg_grade, avg_stars) = self.calculate_averages();
        ProblemSummary {
            id: self.id,
            name: self.name.clone(),
            description: self.description.clone(),
            author: self.author.clone(),
            grade: self.grade,
            sector_name: self.sector_name.clone(),
            average_grade: avg_grade,
            average_stars: avg_stars,
            updated_at: self.updated_at.clone(),
        }
    }

    pub fn to_detail(&self) -> ProblemDetail {
        let (avg_grade, avg_stars) = self.calculate_averages();
        ProblemDetail {
            id: self.id,
            name: self.name.clone(),
            description: self.description.clone(),
            author: self.author.clone(),
            grade: self.grade,
            sector_name: self.sector_name.clone(),
            hold_sequence: self.hold_sequence.clone(),
            average_grade: avg_grade,
            average_stars: avg_stars,
            updated_at: self.updated_at.clone(),
        }
    }
}
