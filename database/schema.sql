CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) COLLATE utf8mb4_0900_ai_ci NOT NULL UNIQUE,
    avg_cycle_length DECIMAL(4, 1) NULL,
    avg_period_length DECIMAL(4, 1) NULL,
    created_at DATE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL,
    CHECK (role IN ('user', 'admin')),
    CHECK (avg_cycle_length IS NULL OR avg_cycle_length > 0),
    CHECK (avg_period_length IS NULL OR avg_period_length > 0),
    INDEX (created_at)
)

CREATE TABLE cycles (
    cycle_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    start_date DATE NOT NULL,
    period_end_date DATE NULL,
    cycle_length INT NULL,
    notes VARCHAR(255) NULL,
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    UNIQUE (user_id, start_date),
    INDEX (start_date),
    CHECK (period_end_date IS NULL OR period_end_date >= start_date),
    CHECK (cycle_length IS NULL OR cycle_length > 0)
)

CREATE TABLE daily_logs (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    log_date DATE NOT NULL,
    flow_level VARCHAR(10) NOT NULL,
    mood VARCHAR(15) NOT NULL,
    pain_level INT NOT NULL,
    notes VARCHAR(255) NULL,
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    UNIQUE (user_id, log_date),
    INDEX (log_date, flow_level),
    CHECK (flow_level IN ('none', 'light', 'medium', 'heavy')),
    CHECK (mood IN ('happy', 'calm', 'irritable', 'anxious', 'sad')),
    CHECK (pain_level BETWEEN 0 AND 10)
)

CREATE TABLE symptoms (
    symptom_id INT PRIMARY KEY AUTO_INCREMENT,
    log_id INT NOT NULL,
    symptom_type VARCHAR(20) NOT NULL,
    severity INT NOT NULL,
    FOREIGN KEY (log_id) REFERENCES daily_logs (log_id) ON DELETE CASCADE,
    UNIQUE (log_id, symptom_type),
    INDEX (symptom_type, log_id),
    CHECK (symptom_type IN ('cramps', 'headache', 'bloating', 'fatigue', 'nausea', 'backache')),
    CHECK (severity BETWEEN 1 AND 5)
)

CREATE TABLE cycle_predictions (
    prediction_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    prediction_date DATE NOT NULL,
    predicted_period_start DATE NOT NULL,
    predicted_period_end DATE NOT NULL,
    prediction_method VARCHAR(50) NOT NULL,
    confidence_range INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    INDEX (user_id, prediction_date),
    INDEX (prediction_date),
    CHECK (predicted_period_start > prediction_date),
    CHECK (predicted_period_end >= predicted_period_start),
    CHECK (confidence_range >= 0)
)
