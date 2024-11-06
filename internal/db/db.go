package db

import (
	"database/sql"
	"fmt"

	_ "github.com/lib/pq"

	"github.com/anhilmy/website-backend/internal"
)

var DB *sql.DB

func ConnectDatabase(conf internal.Config) error {
	psqlUrl := fmt.Sprintf("host=%s port=%d user=%s dbname=%s password=%s sslmode=disable",
		conf.DbHost, conf.DbPort, conf.DbUser, conf.DbName, conf.DbPassword,
	)
	db, err := sql.Open("postgres", psqlUrl)
	if err != nil {
		return err
	}
	DB = db
	return nil
}
