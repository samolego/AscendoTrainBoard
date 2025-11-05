use rand::Rng;
use sha2::{Digest, Sha256};
use std::collections::HashMap;

const TOKEN_PREFIX: &str = "Bearer ";

pub fn generate_salt() -> String {
    let mut rng = rand::thread_rng();
    let salt_bytes: [u8; 32] = rng.r#gen();
    hex::encode(salt_bytes)
}

pub fn hash_password(password: &str, salt: &str) -> String {
    let mut hasher = Sha256::new();
    hasher.update(password.as_bytes());
    hasher.update(salt.as_bytes());
    format!("{:x}", hasher.finalize())
}

pub fn verify_password(password: &str, salt: &str, hash: &str) -> bool {
    hash_password(password, salt) == hash
}

pub fn generate_token() -> String {
    let mut rng = rand::thread_rng();
    let token_bytes: [u8; 32] = rng.r#gen();
    hex::encode(token_bytes)
}

pub fn extract_token(auth_header: Option<&str>) -> Option<String> {
    auth_header
        .and_then(|h| h.strip_prefix(TOKEN_PREFIX))
        .map(|s| s.to_string())
}

pub struct SessionManager {
    sessions: HashMap<String, String>, // token -> username
}

impl SessionManager {
    pub fn new() -> Self {
        Self {
            sessions: HashMap::new(),
        }
    }

    pub fn create_session(&mut self, username: String) -> String {
        let token = generate_token();
        self.sessions.insert(token.clone(), username);
        token
    }

    pub fn get_username(&self, token: &str) -> Option<&String> {
        self.sessions.get(token)
    }

    pub fn remove_session(&mut self, token: &str) -> Option<String> {
        self.sessions.remove(token)
    }
}
