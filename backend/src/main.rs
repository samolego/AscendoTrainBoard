use axum::{
    Router,
    extract::ConnectInfo,
    http::{Request, Response, header},
    routing::{delete, get, post, put},
};
use std::net::SocketAddr;
use std::path::PathBuf;
use std::time::Duration;
use tower_http::cors::{Any, CorsLayer};
use tower_http::services::ServeDir;
use tower_http::trace::TraceLayer;
use tracing::info;

mod auth;
mod handlers;
mod models;
mod rate_limit;
mod state;

use state::AppState;

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt()
        .with_target(false)
        .compact()
        .init();

    let data_path = PathBuf::from("./data");
    let page_path = PathBuf::from("./page");
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
        .allow_headers(vec![header::AUTHORIZATION, header::CONTENT_TYPE]);

    const API_V1_AUTH: &str = "/api/v1/auth";
    const API_V1_SECTORS: &str = "/api/v1/sectors";
    const API_V1_SECTORS_ID: &str = "/api/v1/sectors/{id}";
    const API_V1_PROBLEMS: &str = "/api/v1/problems";
    const API_V1_PROBLEMS_ID: &str = "/api/v1/problems/{id}";

    let app = Router::new()
        .fallback_service(ServeDir::new(page_path))
        .route(
            &format!("{}/register", API_V1_AUTH),
            post(handlers::register),
        )
        .route(&format!("{}/login", API_V1_AUTH), post(handlers::login))
        .route(&format!("{}/logout", API_V1_AUTH), post(handlers::logout))
        .route(
            &format!("{}/rotate_token", API_V1_AUTH),
            get(handlers::rotate_token),
        )
        .route(API_V1_SECTORS, get(handlers::list_sectors))
        .route(API_V1_SECTORS_ID, get(handlers::get_sector))
        .route(
            &format!("{}/image", API_V1_SECTORS_ID),
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
        .layer(
            TraceLayer::new_for_http()
                .on_request(|request: &Request<_>, _span: &tracing::Span| {
                    let method = request.method();
                    let uri = request.uri().path();
                    let ip = request
                        .extensions()
                        .get::<ConnectInfo<SocketAddr>>()
                        .map(|ConnectInfo(addr)| addr.ip().to_string())
                        .unwrap_or_else(|| "unknown".to_string());
                    info!("{} {} from {}", method, uri, ip);
                })
                .on_response(
                    |response: &Response<_>, latency: Duration, _span: &tracing::Span| {
                        let status = response.status();
                        info!(
                            "response {} in {:?}: {:?}",
                            status,
                            latency,
                            response.body()
                        );
                    },
                ),
        )
        .layer(cors);

    let port = if cfg!(debug_assertions) { 3000 } else { 80 };
    let addr = format!("0.0.0.0:{}", port);

    let listener = tokio::net::TcpListener::bind(&addr)
        .await
        .unwrap_or_else(|_| panic!("Failed to bind to port {}", port));

    println!("Server running on http://0.0.0.0:{}", port);
    println!("API available at http://0.0.0.0:{}/api/v1", port);

    axum::serve(
        listener,
        app.into_make_service_with_connect_info::<SocketAddr>(),
    )
    .await
    .expect("Server failed to start");
}
