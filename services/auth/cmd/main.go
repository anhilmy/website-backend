package main

import (
	"github.com/anhilmy/website-backend/internal/shared/db"
	"github.com/anhilmy/website-backend/services/auth/internal"

	"github.com/gin-gonic/gin"
)

func main() {
	// Initialize the database connection
	database, err := db.InitDB()
	if err != nil {
		panic("Failed to connect to the database: " + err.Error())
	}

	// Initialize the Gin router
	router := gin.Default()

	// Set up the handler with the database connection
	internal.Handler(router, database)

	// Start the Gin server
	router.Run(":8080")
}
