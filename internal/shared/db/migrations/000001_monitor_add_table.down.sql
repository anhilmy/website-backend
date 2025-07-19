DROP TABLE IF EXISTS cpu_temperature;
DROP TABLE IF EXISTS memory_usage;
DROP TABLE IF EXISTS cpu_usage;
DROP TABLE IF EXISTS disk_usage;
DROP TABLE IF EXISTS network_io;
DROP TABLE IF EXISTS process_stats;

DROP INDEX IF EXISTS idx_cpu_temperature_timestamp;
DROP INDEX IF EXISTS idx_memory_usage_timestamp;
DROP INDEX IF EXISTS idx_cpu_usage_timestamp;
DROP INDEX IF EXISTS idx_disk_usage_timestamp;
DROP INDEX IF EXISTS idx_network_io_timestamp;
DROP INDEX IF EXISTS idx_process_stats_timestamp;

DROP INDEX IF EXISTS idx_cpu_temperature_hostname;
DROP INDEX IF EXISTS idx_memory_usage_hostname;
DROP INDEX IF EXISTS idx_cpu_usage_hostname;
DROP INDEX IF EXISTS idx_disk_usage_hostname;
DROP INDEX IF EXISTS idx_network_io_hostname;
DROP INDEX IF EXISTS idx_process_stats_hostname;