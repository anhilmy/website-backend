package internal

import "database/sql"

type Repository interface{}

type repository struct {
	db *sql.DB
}

func NewRepository(db *sql.DB) Repository {
	return repository{db}
}
