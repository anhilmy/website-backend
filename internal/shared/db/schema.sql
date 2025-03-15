-- Server monitoring tables

-- CPU Temperature monitoring
CREATE TABLE IF NOT EXISTS cpu_temperature (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    temperature_celsius DECIMAL(5,2) NOT NULL,
    cpu_index INTEGER NOT NULL,
    hostname VARCHAR(255) NOT NULL
);

-- Memory Usage monitoring
CREATE TABLE IF NOT EXISTS memory_usage (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_memory_bytes BIGINT NOT NULL,
    used_memory_bytes BIGINT NOT NULL,
    free_memory_bytes BIGINT NOT NULL,
    memory_usage_percent DECIMAL(5,2) NOT NULL,
    swap_total_bytes BIGINT NOT NULL,
    swap_used_bytes BIGINT NOT NULL,
    swap_free_bytes BIGINT NOT NULL,
    hostname VARCHAR(255) NOT NULL
);

-- CPU Usage monitoring
CREATE TABLE IF NOT EXISTS cpu_usage (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cpu_index INTEGER NOT NULL,
    user_percent DECIMAL(5,2) NOT NULL,
    system_percent DECIMAL(5,2) NOT NULL,
    idle_percent DECIMAL(5,2) NOT NULL,
    nice_percent DECIMAL(5,2) NOT NULL,
    iowait_percent DECIMAL(5,2) NOT NULL,
    irq_percent DECIMAL(5,2) NOT NULL,
    softirq_percent DECIMAL(5,2) NOT NULL,
    steal_percent DECIMAL(5,2) NOT NULL,
    guest_percent DECIMAL(5,2) NOT NULL,
    hostname VARCHAR(255) NOT NULL
);

-- Disk Usage monitoring
CREATE TABLE IF NOT EXISTS disk_usage (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    path VARCHAR(255) NOT NULL,
    total_bytes BIGINT NOT NULL,
    used_bytes BIGINT NOT NULL,
    free_bytes BIGINT NOT NULL,
    usage_percent DECIMAL(5,2) NOT NULL,
    inodes_total BIGINT NOT NULL,
    inodes_used BIGINT NOT NULL,
    inodes_free BIGINT NOT NULL,
    hostname VARCHAR(255) NOT NULL
);

-- Network I/O monitoring
CREATE TABLE IF NOT EXISTS network_io (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    interface_name VARCHAR(255) NOT NULL,
    bytes_sent BIGINT NOT NULL,
    bytes_recv BIGINT NOT NULL,
    packets_sent BIGINT NOT NULL,
    packets_recv BIGINT NOT NULL,
    errin BIGINT NOT NULL,
    errout BIGINT NOT NULL,
    dropin BIGINT NOT NULL,
    dropout BIGINT NOT NULL,
    hostname VARCHAR(255) NOT NULL
);

-- Process monitoring
CREATE TABLE IF NOT EXISTS process_stats (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pid INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    cpu_percent DECIMAL(5,2) NOT NULL,
    memory_percent DECIMAL(5,2) NOT NULL,
    num_threads INTEGER NOT NULL,
    num_fds INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    hostname VARCHAR(255) NOT NULL
);

-- Create indexes for better query performance
CREATE INDEX idx_cpu_temperature_timestamp ON cpu_temperature(timestamp);
CREATE INDEX idx_memory_usage_timestamp ON memory_usage(timestamp);
CREATE INDEX idx_cpu_usage_timestamp ON cpu_usage(timestamp);
CREATE INDEX idx_disk_usage_timestamp ON disk_usage(timestamp);
CREATE INDEX idx_network_io_timestamp ON network_io(timestamp);
CREATE INDEX idx_process_stats_timestamp ON process_stats(timestamp);

-- Create indexes for hostname to filter by server
CREATE INDEX idx_cpu_temperature_hostname ON cpu_temperature(hostname);
CREATE INDEX idx_memory_usage_hostname ON memory_usage(hostname);
CREATE INDEX idx_cpu_usage_hostname ON cpu_usage(hostname);
CREATE INDEX idx_disk_usage_hostname ON disk_usage(hostname);
CREATE INDEX idx_network_io_hostname ON network_io(hostname);
CREATE INDEX idx_process_stats_hostname ON process_stats(hostname);
