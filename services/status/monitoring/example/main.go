package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/anhilmy/website-backend/internal/shared/config"
	"github.com/anhilmy/website-backend/internal/shared/db"
	"github.com/anhilmy/website-backend/services/status/monitoring"
)

func main() {
	// Load configuration
	cfg, err := config.LoadMonitoringConfig("config.yaml")
	if err != nil {
		log.Fatalf("Failed to load config: %v", err)
	}

	// Initialize database connection
	err = db.InitDB()
	if err != nil {
		log.Fatalf("Failed to initialize database: %v", err)
	}
	db := db.DB
	defer db.Close()

	// Create monitoring service
	monitor := monitoring.NewMonitoringService(cfg, db)

	// Create context that can be cancelled
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Start monitoring service
	if err := monitor.Start(ctx); err != nil {
		log.Fatalf("Failed to start monitoring service: %v", err)
	}

	// Wait for interrupt signal
	sigChan := make(chan os.Signal, 1)
	signal.Notify(sigChan, syscall.SIGINT, syscall.SIGTERM)
	<-sigChan

	// Graceful shutdown
	log.Println("Shutting down monitoring service...")
	cancel()
	monitor.Stop()
	log.Println("Monitoring service stopped")
}
