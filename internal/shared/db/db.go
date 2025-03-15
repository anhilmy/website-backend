package db

import (
	"database/sql"
	_ "github.com/mattn/go-sqlite3"
	"log"
	"os"
	"path/filepath"
)

var DB *sql.DB

func InitDB() error {
	// Ensure internal/db directory exists
	dbDir := "internal/shared/db"
	if err := os.MkdirAll(dbDir, 0755); err != nil {
		return err
	}	

	// Open SQLite database
	dbPath := filepath.Join(dbDir, "status.db")
	var err error
	DB, err = sql.Open("sqlite3", dbPath)
	if err != nil {
		return err
	}

	if err != nil {
		return err
	}

	return nil
}