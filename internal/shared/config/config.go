package config

import (
	"fmt"
	"os"
	"time"

	"gopkg.in/yaml.v3"
)

type Config struct {
	DbHost     string
	DbName     string
	DbPassword string
	DbUsername string
	DbPort     string

	RedisHost     string
	RedisPort     string
	RedisPassword string
}

func LoadConfig(path string) (cfg *Config, err error) {

	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("error reading config file: %w", err)
	}

	if err := yaml.Unmarshal(data, &cfg); err != nil {
		return nil, fmt.Errorf("error parsing config file: %w", err)
	}

	return
}

type MonitoringConfig struct {
	Intervals struct {
		CPUTemperature time.Duration `yaml:"cpu_temperature"`
		MemoryUsage    time.Duration `yaml:"memory_usage"`
		CPUUsage       time.Duration `yaml:"cpu_usage"`
		DiskUsage      time.Duration `yaml:"disk_usage"`
		NetworkIO      time.Duration `yaml:"network_io"`
		ProcessStats   time.Duration `yaml:"process_stats"`
	} `yaml:"intervals"`
	Hostname string `yaml:"hostname"`
	Config   *Config
}

func LoadMonitoringConfig(path string) (*MonitoringConfig, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("error reading config file: %w", err)
	}

	var config MonitoringConfig
	if err := yaml.Unmarshal(data, &config); err != nil {
		return nil, fmt.Errorf("error parsing config file: %w", err)
	}

	// Set default hostname if not provided
	if config.Hostname == "" {
		hostname, err := os.Hostname()
		if err != nil {
			return nil, fmt.Errorf("error getting hostname: %w", err)
		}
		config.Hostname = hostname
	}

	config.Config, err = LoadConfig(path)
	if err != nil {
		return nil, fmt.Errorf("error reading config file: %w", err)
	}

	return &config, nil
}
