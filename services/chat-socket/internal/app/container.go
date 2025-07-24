package app

import (
	"database/sql"
	"log"
)

type Container struct {
	DB     *sql.DB
	Logger log.Logger
	Config shared.Config
}
