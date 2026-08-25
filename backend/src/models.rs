use serde::{Deserialize, Serialize};
use serde_repr::{Deserialize_repr, Serialize_repr};
use std::time::{SystemTime, UNIX_EPOCH};

use crate::state::AppState;

// Settings (read-only, loaded on startup)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Settings {
    pub admin_users: Vec<String>,
}

// User
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub username: String,
    pub password_hash: String,
    pub salt: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Username(pub String);

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserDetail {
    pub username: String,
    pub is_admin: bool,
}

impl UserDetail {
    pub fn from_user(u: &User, state: &AppState) -> Self {
        let username = u.username.clone();
        let is_admin = state.is_admin(&username);
        UserDetail { username, is_admin }
    }
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

#[derive(Debug, Deserialize)]
pub struct ChangePasswordRequest {
    pub old_password: String,
    pub new_password: String,
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
    #[serde(default)]
    pub is_competition: bool,
    #[serde(default)]
    pub winner: bool,
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
    pub is_competition: Option<bool>,
}

#[derive(Debug, Deserialize)]
pub struct UpdateProblemRequest {
    pub name: Option<String>,
    pub description: Option<String>,
    pub grade: Option<u8>,
    pub hold_sequence: Option<Vec<Hold>>,
    pub is_competition: Option<bool>,
    pub winner: Option<bool>,
}

// Grade
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Grade {
    pub username: String,
    pub grade: u8,
    pub attempt: Attempt,
    pub stars: u8,
    pub created_at: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum Attempt {
    Fail,
    Flash,
    Redpoint,
}

#[derive(Debug, Deserialize)]
pub struct SubmitGradeRequest {
    pub grade: u8,
    pub stars: u8,
    pub attempt: Attempt,
}

#[derive(Debug, Serialize, Deserialize)]
pub enum Tag {
    Tekmovalni(bool),             // je tekmovalni za ta mesec ali ne
    Zmagovalni(bool),             // je zmagal na tekmovanju
    Avtor(String),                // Avtor bolderja
    SpremenjeniZaDatumom(String), // Bolderji, spremenjeni za datumom
    Splezani(Attempt),            // Bolderji z mojo oceno poskusa
    Ime(String),                  // Bolderji z imenom, ki vsebuje podan niz
    MinGrade(u8),                 // Bolderji z oceno večjo ali enako navedeni
    MaxGrade(u8),                 // Bolderji z oceno manjšo ali enako navedeni
    SectorId(u16),                // Bolderji v določenem sektorju
}

#[derive(Debug, Serialize, Deserialize)]
pub struct PossiblyNegatedTag {
    #[serde(default)]
    pub negated: bool,
    #[serde(flatten)]
    pub tag: Tag,
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

    pub fn get_problem_score(&self) -> f32 {
        let (avg_grade, avg_stars) = self.calculate_averages();
        let grade_difference = self.base.grade as f32 - avg_grade.unwrap_or(self.base.grade.into());
        let has_name = if self.base.name.trim().is_empty() {
            0.0
        } else {
            1.0
        };

        avg_stars.unwrap_or(3.0) * 5.0 - grade_difference.powf(2.0) + has_name
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

    pub fn has_tag(&self, ptag: &PossiblyNegatedTag, user: &Option<String>) -> bool {
        let PossiblyNegatedTag { tag, negated } = ptag;
        return negated
            ^ match tag {
                Tag::Tekmovalni(is_comp) => {
                    if *is_comp {
                        if !self.base.is_competition {
                            return false;
                        }
                        if let Ok(ts) = self.base.updated_at.parse::<u64>() {
                            if let Ok(now) = SystemTime::now().duration_since(UNIX_EPOCH) {
                                return is_same_month(ts, now.as_secs());
                            }
                        }
                        false
                    } else {
                        !self.base.is_competition
                    }
                }
                Tag::Zmagovalni(is_winner) => {
                    *is_winner && self.base.winner || !*is_winner && !self.base.winner
                }
                Tag::Avtor(author) => self.base.author.contains(author),
                Tag::Splezani(poskus) => self.grades.iter().any(|g| match user {
                    Some(username) => &g.username == username && &g.attempt == poskus,
                    None => &g.attempt == poskus,
                }),
                Tag::SpremenjeniZaDatumom(timestamp) => self.base.updated_at >= *timestamp,
                Tag::Ime(name) => self.base.name.contains(name),
                Tag::MinGrade(g) => self.base.grade >= *g,
                Tag::MaxGrade(g) => self.base.grade <= *g,
                Tag::SectorId(id) => self.base.sector_id == *id,
            };
    }
}

fn is_same_month(ts1: u64, ts2: u64) -> bool {
    let (y1, m1) = to_year_month(ts1);
    let (y2, m2) = to_year_month(ts2);
    y1 == y2 && m1 == m2
}

fn to_year_month(ts: u64) -> (u64, u64) {
    let seconds_per_day = 86400;
    let mut days = ts / seconds_per_day;
    let mut year = 1970;
    loop {
        let is_leap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
        let days_in_year = if is_leap { 366 } else { 365 };
        if days < days_in_year {
            break;
        }
        days -= days_in_year;
        year += 1;
    }

    let is_leap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    let days_in_months = [
        31,
        if is_leap { 29 } else { 28 },
        31,
        30,
        31,
        30,
        31,
        31,
        30,
        31,
        30,
        31,
    ];

    let mut month = 0;
    for (i, &dim) in days_in_months.iter().enumerate() {
        if days < dim {
            month = i as u64;
            break;
        }
        days -= dim;
    }
    (year, month)
}
