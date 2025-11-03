use axum::{
    Json,
    extract::{ConnectInfo, Path, Query, State},
    http::{HeaderMap, StatusCode, header},
    response::{IntoResponse, Response},
};
use serde::Deserialize;
use std::net::SocketAddr;

use crate::auth::{extract_token, generate_salt, hash_password, verify_password};
use crate::models::*;

use crate::state::AppState;

// Helper to get current timestamp
fn now() -> String {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_secs()
        .to_string()
}

// Helper to get authenticated user
async fn get_auth_user(state: &AppState, headers: &HeaderMap) -> Result<String, Response> {
    let auth_header = headers
        .get(header::AUTHORIZATION)
        .and_then(|h| h.to_str().ok());
    let token = extract_token(auth_header).ok_or_else(|| {
        (
            StatusCode::UNAUTHORIZED,
            Json(ErrorResponse {
                error: "Not authenticated".to_string(),
                code: "NOT_AUTHENTICATED".to_string(),
                timeout: None,
            }),
        )
            .into_response()
    })?;

    let sessions = state.sessions.read().await;
    let username = sessions.get_username(&token).ok_or_else(|| {
        (
            StatusCode::UNAUTHORIZED,
            Json(ErrorResponse {
                error: "Invalid token".to_string(),
                code: "INVALID_TOKEN".to_string(),
                timeout: None,
            }),
        )
            .into_response()
    })?;

    Ok(username.clone())
}

// Auth handlers
pub async fn register(
    State(state): State<AppState>,
    Json(payload): Json<RegisterRequest>,
) -> Result<impl IntoResponse, (StatusCode, Json<ErrorResponse>)> {
    if payload.username.len() < 3 || payload.username.len() > 50 {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(ErrorResponse {
                error: "Username must be between 3 and 50 characters".to_string(),
                code: "INVALID_USERNAME".to_string(),
                timeout: None,
            }),
        ));
    }

    if payload.password.len() < 6 {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(ErrorResponse {
                error: "Password must be at least 6 characters".to_string(),
                code: "INVALID_PASSWORD".to_string(),
                timeout: None,
            }),
        ));
    }

    let mut users = state.users.write().await;

    if users.iter().any(|u| u.username == payload.username) {
        return Err((
            StatusCode::CONFLICT,
            Json(ErrorResponse {
                error: "Username already exists".to_string(),
                code: "USERNAME_EXISTS".to_string(),
                timeout: None,
            }),
        ));
    }

    let salt = generate_salt();
    let password_hash = hash_password(&payload.password, &salt);

    let user = User {
        username: payload.username.clone(),
        password_hash,
        salt,
    };

    users.push(user);
    drop(users);

    state.mark_dirty();

    let mut sessions = state.sessions.write().await;
    let token = sessions.create_session(payload.username.clone());

    Ok((
        StatusCode::CREATED,
        Json(LoginResponse {
            token,
            username: payload.username,
        }),
    ))
}

pub async fn login(
    State(state): State<AppState>,
    ConnectInfo(addr): ConnectInfo<SocketAddr>,
    Json(payload): Json<LoginRequest>,
) -> Result<impl IntoResponse, (StatusCode, Json<ErrorResponse>)> {
    let ip = addr.ip();

    let mut rate_limiter = state.rate_limiter.write().await;
    if let Err(rate_error) = rate_limiter.check_and_wait(ip) {
        return Err((
            StatusCode::TOO_MANY_REQUESTS,
            Json(ErrorResponse {
                error: rate_error.message().to_string(),
                code: rate_error.code().to_string(),
                timeout: Some(rate_error.timeout()),
            }),
        ));
    }
    drop(rate_limiter);

    let users = state.users.read().await;

    let user = users.iter().find(|u| u.username == payload.username);

    let user = match user {
        Some(u) => u,
        None => {
            drop(users);
            let mut rate_limiter = state.rate_limiter.write().await;
            let timeout = rate_limiter.record_failed_attempt(ip);
            return Err((
                StatusCode::UNAUTHORIZED,
                Json(ErrorResponse {
                    error: "Invalid credentials".to_string(),
                    code: "INVALID_CREDENTIALS".to_string(),
                    timeout: Some(timeout),
                }),
            ));
        }
    };

    if !verify_password(&payload.password, &user.salt, &user.password_hash) {
        drop(users);
        let mut rate_limiter = state.rate_limiter.write().await;
        let timeout = rate_limiter.record_failed_attempt(ip);
        return Err((
            StatusCode::UNAUTHORIZED,
            Json(ErrorResponse {
                error: "Invalid credentials".to_string(),
                code: "INVALID_CREDENTIALS".to_string(),
                timeout: Some(timeout),
            }),
        ));
    }

    let username = user.username.clone();
    drop(users);

    let mut rate_limiter = state.rate_limiter.write().await;
    rate_limiter.record_successful_attempt(ip);
    drop(rate_limiter);

    let mut sessions = state.sessions.write().await;
    let token = sessions.create_session(username.clone());

    Ok(Json(LoginResponse { token, username }))
}

pub async fn logout(
    State(state): State<AppState>,
    headers: HeaderMap,
) -> Result<impl IntoResponse, (StatusCode, Json<ErrorResponse>)> {
    let auth_header = headers
        .get(header::AUTHORIZATION)
        .and_then(|h| h.to_str().ok());
    let token = extract_token(auth_header).ok_or_else(|| {
        (
            StatusCode::UNAUTHORIZED,
            Json(ErrorResponse {
                error: "Not authenticated".to_string(),
                code: "NOT_AUTHENTICATED".to_string(),
                timeout: None,
            }),
        )
    })?;

    let mut sessions = state.sessions.write().await;
    sessions.remove_session(&token);

    Ok(StatusCode::NO_CONTENT)
}

// Sector handlers
pub async fn list_sectors(
    State(state): State<AppState>,
) -> Result<Json<Vec<SectorSummary>>, (StatusCode, Json<ErrorResponse>)> {
    Ok(Json(state.sectors.clone()))
}

pub async fn get_sector(
    State(state): State<AppState>,
    Path(name): Path<String>,
) -> Result<Json<Sector>, (StatusCode, Json<ErrorResponse>)> {
    let metadata = state.sector_metadata.get(&name).ok_or_else(|| {
        (
            StatusCode::NOT_FOUND,
            Json(ErrorResponse {
                error: "Sector not found".to_string(),
                code: "NOT_FOUND".to_string(),
                timeout: None,
            }),
        )
    })?;

    Ok(Json(Sector {
        name: name,
        holds: metadata.holds.clone(),
    }))
}

pub async fn get_sector_image(
    State(state): State<AppState>,
    Path(name): Path<String>,
) -> Result<Response, (StatusCode, Json<ErrorResponse>)> {
    let metadata = state.sector_metadata.get(&name).ok_or_else(|| {
        (
            StatusCode::NOT_FOUND,
            Json(ErrorResponse {
                error: "Sector not found".to_string(),
                code: "NOT_FOUND".to_string(),
                timeout: None,
            }),
        )
    })?;

    let sector_dir = state.sectors_path.join(&name);
    let image_path = sector_dir.join(&metadata.image_filename);
    if !image_path.exists() {
        return Err((
            StatusCode::NOT_FOUND,
            Json(ErrorResponse {
                error: "Sector image not found".to_string(),
                code: "NOT_FOUND".to_string(),
                timeout: None,
            }),
        ));
    }

    let image_data = tokio::fs::read(&image_path).await.map_err(|_| {
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            Json(ErrorResponse {
                error: "Failed to read sector image".to_string(),
                code: "IO_ERROR".to_string(),
                timeout: None,
            }),
        )
    })?;

    let content_type = if metadata.image_filename.ends_with(".png") {
        "image/png"
    } else if metadata.image_filename.ends_with(".jpg")
        || metadata.image_filename.ends_with(".jpeg")
    {
        "image/jpeg"
    } else {
        "application/octet-stream"
    };

    Ok(([(header::CONTENT_TYPE, content_type)], image_data).into_response())
}

// Problem handlers
#[derive(Debug, Deserialize)]
pub struct ProblemQuery {
    pub sector: Option<String>,
    pub min_grade: Option<u8>,
    pub max_grade: Option<u8>,
    pub name: Option<String>,
    pub page: Option<u32>,
    pub per_page: Option<u32>,
}

pub async fn list_problems(
    State(state): State<AppState>,
    Query(query): Query<ProblemQuery>,
) -> Result<Json<ProblemList>, (StatusCode, Json<ErrorResponse>)> {
    let problems = state.problems.read().await;

    let filtered: Vec<&Problem> = problems
        .iter()
        .filter(|p| query.sector.as_ref().map_or(true, |s| p.sector_name == *s))
        .filter(|p| query.min_grade.map_or(true, |g| p.grade >= g))
        .filter(|p| query.max_grade.map_or(true, |g| p.grade <= g))
        .filter(|p| {
            query.name.as_ref().map_or(true, |name| {
                p.name.to_lowercase().contains(&name.to_lowercase())
            })
        })
        .collect();

    let total = filtered.len() as u32;
    let page = query.page.unwrap_or(1).max(1);
    let per_page = query.per_page.unwrap_or(20).min(100).max(1);

    let skip = ((page - 1) * per_page) as usize;
    let summaries: Vec<ProblemSummary> = filtered
        .into_iter()
        .skip(skip)
        .take(per_page as usize)
        .map(|p| p.to_summary())
        .collect();

    Ok(Json(ProblemList {
        problems: summaries,
        total,
        page,
        per_page,
    }))
}

pub async fn get_problem(
    State(state): State<AppState>,
    Path(id): Path<u32>,
) -> Result<Json<ProblemDetail>, (StatusCode, Json<ErrorResponse>)> {
    let problems = state.problems.read().await;

    let problem = problems.iter().find(|p| p.id == id).ok_or_else(|| {
        (
            StatusCode::NOT_FOUND,
            Json(ErrorResponse {
                error: "Problem not found".to_string(),
                code: "NOT_FOUND".to_string(),
                timeout: None,
            }),
        )
    })?;

    Ok(Json(problem.to_detail()))
}

pub async fn create_problem(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(payload): Json<CreateProblemRequest>,
) -> Result<impl IntoResponse, Response> {
    let username = get_auth_user(&state, &headers).await?;

    if !state.sector_metadata.contains_key(&payload.sector_name) {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(ErrorResponse {
                error: "Sector does not exist".to_string(),
                code: "INVALID_SECTOR".to_string(),
                timeout: None,
            }),
        )
            .into_response());
    }

    if payload.hold_sequence.is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(ErrorResponse {
                error: "Hold sequence cannot be empty".to_string(),
                code: "INVALID_HOLD_SEQUENCE".to_string(),
                timeout: None,
            }),
        )
            .into_response());
    }

    let id = state.get_next_problem_id().await;
    let name = payload.name.unwrap_or_else(|| format!("Problem {}", id));

    let problem = Problem {
        id,
        name,
        description: payload.description,
        author: username,
        grade: payload.grade,
        sector_name: payload.sector_name,
        hold_sequence: payload.hold_sequence,
        grades: Vec::new(),
        updated_at: now(),
    };

    let detail = problem.to_detail();

    let mut problems = state.problems.write().await;
    problems.push(problem);
    drop(problems);

    state.mark_dirty();

    Ok((StatusCode::CREATED, Json(detail)).into_response())
}

pub async fn update_problem(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<u32>,
    Json(payload): Json<UpdateProblemRequest>,
) -> Result<impl IntoResponse, Response> {
    let username = get_auth_user(&state, &headers).await?;

    let mut problems = state.problems.write().await;

    let problem = problems.iter_mut().find(|p| p.id == id).ok_or_else(|| {
        (
            StatusCode::NOT_FOUND,
            Json(ErrorResponse {
                error: "Problem not found".to_string(),
                code: "NOT_FOUND".to_string(),
                timeout: None,
            }),
        )
            .into_response()
    })?;

    let is_admin = state.is_admin(&username);
    if problem.author != username && !is_admin {
        return Err((
            StatusCode::FORBIDDEN,
            Json(ErrorResponse {
                error: "You can only edit your own problems".to_string(),
                code: "FORBIDDEN".to_string(),
                timeout: None,
            }),
        )
            .into_response());
    }

    if let Some(ref seq) = payload.hold_sequence {
        if seq.is_empty() {
            return Err((
                StatusCode::BAD_REQUEST,
                Json(ErrorResponse {
                    error: "Hold sequence cannot be empty".to_string(),
                    code: "INVALID_HOLD_SEQUENCE".to_string(),
                    timeout: None,
                }),
            )
                .into_response());
        }
    }

    if let Some(name) = payload.name {
        problem.name = name;
    }
    if let Some(description) = payload.description {
        problem.description = Some(description);
    }
    if let Some(grade) = payload.grade {
        problem.grade = grade;
    }
    if let Some(hold_sequence) = payload.hold_sequence {
        problem.hold_sequence = hold_sequence;
        // Clear grades if hold sequence changes
        problem.grades.clear();
    }

    problem.updated_at = now();

    let detail = problem.to_detail();
    drop(problems);

    state.mark_dirty();

    Ok(Json(detail).into_response())
}

pub async fn delete_problem(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<u32>,
) -> Result<impl IntoResponse, Response> {
    let username = get_auth_user(&state, &headers).await?;

    let mut problems = state.problems.write().await;

    let pos = problems.iter().position(|p| p.id == id).ok_or_else(|| {
        (
            StatusCode::NOT_FOUND,
            Json(ErrorResponse {
                error: "Problem not found".to_string(),
                code: "NOT_FOUND".to_string(),
                timeout: None,
            }),
        )
            .into_response()
    })?;

    let is_admin = state.is_admin(&username);
    if problems[pos].author != username && !is_admin {
        return Err((
            StatusCode::FORBIDDEN,
            Json(ErrorResponse {
                error: "You can only delete your own problems".to_string(),
                code: "FORBIDDEN".to_string(),
                timeout: None,
            }),
        )
            .into_response());
    }

    problems.remove(pos);
    drop(problems);

    state.mark_dirty();

    Ok(StatusCode::NO_CONTENT.into_response())
}

// Grade handlers
pub async fn get_problem_grades(
    State(state): State<AppState>,
    Path(id): Path<u32>,
) -> Result<Json<ProblemGrades>, (StatusCode, Json<ErrorResponse>)> {
    let problems = state.problems.read().await;

    let problem = problems.iter().find(|p| p.id == id).ok_or_else(|| {
        (
            StatusCode::NOT_FOUND,
            Json(ErrorResponse {
                error: "Problem not found".to_string(),
                code: "NOT_FOUND".to_string(),
                timeout: None,
            }),
        )
    })?;

    let (avg_grade, avg_stars) = problem.calculate_averages();

    Ok(Json(ProblemGrades {
        problem_id: id,
        grades: problem.grades.clone(),
        average_grade: avg_grade,
        average_stars: avg_stars,
    }))
}

pub async fn submit_problem_grade(
    State(state): State<AppState>,
    headers: HeaderMap,
    Path(id): Path<u32>,
    Json(payload): Json<SubmitGradeRequest>,
) -> Result<impl IntoResponse, Response> {
    let username = get_auth_user(&state, &headers).await?;

    if payload.stars < 1 || payload.stars > 5 {
        return Err((
            StatusCode::BAD_REQUEST,
            Json(ErrorResponse {
                error: "Stars must be between 1 and 5".to_string(),
                code: "INVALID_STARS".to_string(),
                timeout: None,
            }),
        )
            .into_response());
    }

    let mut problems = state.problems.write().await;

    let problem = problems.iter_mut().find(|p| p.id == id).ok_or_else(|| {
        (
            StatusCode::NOT_FOUND,
            Json(ErrorResponse {
                error: "Problem not found".to_string(),
                code: "NOT_FOUND".to_string(),
                timeout: None,
            }),
        )
            .into_response()
    })?;

    let existing_pos = problem.grades.iter().position(|g| g.username == username);

    let (status, grade) = if let Some(pos) = existing_pos {
        problem.grades[pos].grade = payload.grade;
        problem.grades[pos].stars = payload.stars;
        problem.grades[pos].created_at = now();
        (StatusCode::OK, problem.grades[pos].clone())
    } else {
        let grade = Grade {
            username: username.clone(),
            grade: payload.grade,
            stars: payload.stars,
            created_at: now(),
        };
        problem.grades.push(grade.clone());
        (StatusCode::CREATED, grade)
    };

    drop(problems);

    state.mark_dirty();

    Ok((status, Json(grade)).into_response())
}
