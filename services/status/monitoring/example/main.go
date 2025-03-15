package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"

	"website-backend/internal/shared/config"
	"website-backend/internal/shared/db"
	"website-backend/services/status/monitoring"
)

func main() {
	// Load configuration
	cfg, err := config.LoadConfig("config.yaml")
	if err != nil {
		log.Fatalf("Failed to load config: %v", err)
	}

	// Initialize database connection
	db, err := db.NewDB("postgres://user:password@localhost:5432/monitoring?sslmode=disable")
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}
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