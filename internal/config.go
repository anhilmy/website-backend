package internal

import (
	"os"

	"gopkg.in/yaml.v3"
)

type Config struct {
	DbHost     string `yaml:"DB_HOST"`
	DbPort     int64  `yaml:"DB_PORT"`
	DbUser     string `yaml:"DB_USER"`
	DbName     string `yaml:"DB_NAME"`
	DbPassword string `yaml:"DB_PASSWORD"`
}

func (conf *Config) LoadConfig(path string) error {
	yamlFile, err := os.ReadFile(path)
	if err != nil {
		return err
	}
	err = yaml.Unmarshal(yamlFile, conf)
	if err != nil {
		return err
	}

	if err = conf.validate(); err != nil {
		return err
	}

	return nil
}

func (conf Config) validate() error {
	return nil
}
