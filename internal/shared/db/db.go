package db

import (
	"database/sql"
	"os"
	"path/filepath"

	_ "github.com/mattn/go-sqlite3"
)

var db *sql.DB

func InitDB() (*sql.DB, error) {
	// Ensure internal/db directory exists
	dbDir := "internal/shared/db"
	if err := os.MkdirAll(dbDir, 0755); err != nil {
		return nil, err
	}

	// Open SQLite database
	dbPath := filepath.Join(dbDir, "status.db")
	var err error
	db, err = sql.Open("sqlite3", dbPath)
	if err != nil {
		return nil, err
	}

	if err != nil {
		return nil, err
	}

	return db, nil
}
