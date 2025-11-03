use axum::{
    Router,
    routing::{delete, get, post, put},
};
use std::net::SocketAddr;
use std::path::PathBuf;
use std::time::Duration;
use tower_http::cors::{Any, CorsLayer};

mod auth;
mod handlers;
mod models;
mod rate_limit;
mod state;

use state::AppState;

#[tokio::main]
async fn main() {
    let data_path = PathBuf::from("./data");
    let sectors_path = PathBuf::from("./sectors");

    tokio::fs::create_dir_all(&data_path)
        .await
        .expect("Failed to create data directory");
    tokio::fs::create_dir_all(&sectors_path)
        .await
        .expect("Failed to create sectors directory");

    let state = AppState::new(data_path, sectors_path)
        .await
        .expect("Failed to initialize state");

    let state_clone = state.clone();
    tokio::spawn(async move {
        let mut interval = tokio::time::interval(Duration::from_secs(30));
        loop {
            interval.tick().await;
            if let Err(e) = state_clone.save_if_dirty().await {
                eprintln!("Failed to save data: {}", e);
            }
        }
    });

    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);

    const API_V1_AUTH: &str = "/api/v1/auth";
    const API_V1_SECTORS: &str = "/api/v1/sectors";
    const API_V1_SECTORS_NAME: &str = "/api/v1/sectors/{name}";
    const API_V1_PROBLEMS: &str = "/api/v1/problems";
    const API_V1_PROBLEMS_ID: &str = "/api/v1/problems/{id}";

    let app = Router::new()
        .route(
            &format!("{}/register", API_V1_AUTH),
            post(handlers::register),
        )
        .route(&format!("{}/login", API_V1_AUTH), post(handlers::login))
        .route(&format!("{}/logout", API_V1_AUTH), post(handlers::logout))
        .route(API_V1_SECTORS, get(handlers::list_sectors))
        .route(API_V1_SECTORS_NAME, get(handlers::get_sector))
        .route(
            &format!("{}/image", API_V1_SECTORS_NAME),
            get(handlers::get_sector_image),
        )
        .route(API_V1_PROBLEMS, get(handlers::list_problems))
        .route(API_V1_PROBLEMS, post(handlers::create_problem))
        .route(API_V1_PROBLEMS_ID, get(handlers::get_problem))
        .route(API_V1_PROBLEMS_ID, put(handlers::update_problem))
        .route(API_V1_PROBLEMS_ID, delete(handlers::delete_problem))
        .route(
            &format!("{}/grades", API_V1_PROBLEMS_ID),
            get(handlers::get_problem_grades),
        )
        .route(
            &format!("{}/grades", API_V1_PROBLEMS_ID),
            post(handlers::submit_problem_grade),
        )
        .with_state(state)
        .layer(cors);

    let listener = tokio::net::TcpListener::bind("0.0.0.0:3000")
        .await
        .expect("Failed to bind to port 3000");

    println!("🚀 Server running on http://0.0.0.0:3000");
    println!("📚 API available at http://0.0.0.0:3000/api/v1");

    axum::serve(
        listener,
        app.into_make_service_with_connect_info::<SocketAddr>(),
    )
    .await
    .expect("Server failed to start");
}
