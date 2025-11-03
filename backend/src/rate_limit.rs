use std::collections::HashMap;
use std::net::IpAddr;
use std::time::{Duration, SystemTime};

const WAIT_MULTIPLIER: u64 = 3;
const BAN_THRESHOLD: u32 = 5;
const BAN_DURATION: u64 = 2 * 60 * 60;
const CLEANUP_AGE: u64 = 24 * 60 * 60;

#[derive(Debug, Clone)]
struct LoginAttempt {
    count: u32,
    last_attempt: SystemTime,
}

pub struct RateLimiter {
    attempts: HashMap<IpAddr, LoginAttempt>,
}

impl RateLimiter {
    pub fn new() -> Self {
        Self {
            attempts: HashMap::new(),
        }
    }

    pub fn check_and_wait(&mut self, ip: IpAddr) -> Result<(), RateLimitError> {
        let now = SystemTime::now();
        self.cleanup_old_entries(now);

        if let Some(attempt) = self.attempts.get(&ip) {
            if attempt.count >= BAN_THRESHOLD {
                let ban_until = attempt.last_attempt + Duration::from_secs(BAN_DURATION);
                if now < ban_until {
                    let remaining = ban_until.duration_since(now).unwrap_or_default();
                    return Err(RateLimitError::Banned(remaining.as_secs()));
                }
            } else if attempt.count > 0 {
                let wait_duration = Duration::from_secs(WAIT_MULTIPLIER * attempt.count as u64);
                let can_attempt_at = attempt.last_attempt + wait_duration;
                if now < can_attempt_at {
                    let remaining = can_attempt_at.duration_since(now).unwrap_or_default();
                    return Err(RateLimitError::TooManyAttempts(remaining.as_secs()));
                }
            }
        }

        Ok(())
    }

    pub fn record_failed_attempt(&mut self, ip: IpAddr) -> u64 {
        let attempt = self.attempts.entry(ip).or_insert(LoginAttempt {
            count: 0,
            last_attempt: SystemTime::now(),
        });

        attempt.count += 1;
        attempt.last_attempt = SystemTime::now();

        if attempt.count >= BAN_THRESHOLD {
            BAN_DURATION
        } else {
            WAIT_MULTIPLIER * attempt.count as u64
        }
    }

    pub fn record_successful_attempt(&mut self, ip: IpAddr) {
        self.attempts.remove(&ip);
    }

    fn cleanup_old_entries(&mut self, now: SystemTime) {
        self.attempts.retain(|_, attempt| {
            now.duration_since(attempt.last_attempt)
                .unwrap_or_default()
                .as_secs()
                < CLEANUP_AGE
        });
    }
}

#[derive(Debug)]
pub enum RateLimitError {
    Banned(u64),
    TooManyAttempts(u64),
}

impl RateLimitError {
    pub fn message(&self) -> &str {
        match self {
            RateLimitError::Banned(_) => {
                "Too many failed login attempts. Account temporarily banned."
            }
            RateLimitError::TooManyAttempts(_) => "Please wait before trying again",
        }
    }

    pub fn code(&self) -> &str {
        match self {
            RateLimitError::Banned(_) => "BANNED",
            RateLimitError::TooManyAttempts(_) => "RATE_LIMIT",
        }
    }

    pub fn timeout(&self) -> u64 {
        match self {
            RateLimitError::Banned(secs) => *secs,
            RateLimitError::TooManyAttempts(secs) => *secs,
        }
    }
}
