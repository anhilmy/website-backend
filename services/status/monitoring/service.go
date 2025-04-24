package monitoring

import (
	"context"
	"log"
	"sync"
	"time"

	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/disk"
	"github.com/shirou/gopsutil/v3/host"
	"github.com/shirou/gopsutil/v3/mem"
	"github.com/shirou/gopsutil/v3/net"
	"github.com/shirou/gopsutil/v3/process"

	"website-backend/internal/shared/config"
	"website-backend/internal/shared/db"
)

type MonitoringService struct {
	config *config.MonitoringConfig
	db     *db.DB
	wg     sync.WaitGroup
}

func NewMonitoringService(cfg *config.MonitoringConfig, db *db.DB) *MonitoringService {
	return &MonitoringService{
		config: cfg,
		db:     db,
	}
}

func (s *MonitoringService) Start(ctx context.Context) error {
	// Start all monitoring goroutines
	s.wg.Add(6) // One for each monitoring function

	go s.monitorCPUTemperature(ctx)
	go s.monitorMemoryUsage(ctx)
	go s.monitorCPUUsage(ctx)
	go s.monitorDiskUsage(ctx)
	go s.monitorNetworkIO(ctx)
	go s.monitorProcessStats(ctx)

	return nil
}

func (s *MonitoringService) Stop() {
	s.wg.Wait()
}

func (s *MonitoringService) monitorCPUTemperature(ctx context.Context) {
	defer s.wg.Done()
	ticker := time.NewTicker(s.config.Intervals.CPUTemperature)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			temperatures, err := host.SensorsTemperatures()
			if err != nil {
				log.Printf("Error getting CPU temperature: %v", err)
				continue
			}

			for i, temp := range temperatures {
				_, err = s.db.Exec(`
					INSERT INTO cpu_temperature (temperature_celsius, cpu_index, hostname)
					VALUES ($1, $2, $3)
				`, temp.Temperature, i, s.config.Hostname)
				if err != nil {
					log.Printf("Error inserting CPU temperature: %v", err)
				}
			}
		}
	}
}

func (s *MonitoringService) monitorMemoryUsage(ctx context.Context) {
	defer s.wg.Done()
	ticker := time.NewTicker(s.config.Intervals.MemoryUsage)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			v, err := mem.VirtualMemory()
			if err != nil {
				log.Printf("Error getting memory usage: %v", err)
				continue
			}

			_, err = s.db.Exec(`
				INSERT INTO memory_usage (
					total_memory_bytes, used_memory_bytes, free_memory_bytes,
					memory_usage_percent, swap_total_bytes, swap_used_bytes,
					swap_free_bytes, hostname
				) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
			`, v.Total, v.Used, v.Free, v.UsedPercent, v.SwapTotal, v.SwapUsed, v.SwapFree, s.config.Hostname)
			if err != nil {
				log.Printf("Error inserting memory usage: %v", err)
			}
		}
	}
}

func (s *MonitoringService) monitorCPUUsage(ctx context.Context) {
	defer s.wg.Done()
	ticker := time.NewTicker(s.config.Intervals.CPUUsage)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			percentages, err := cpu.Percent(time.Second, true)
			if err != nil {
				log.Printf("Error getting CPU usage: %v", err)
				continue
			}

			for i, p := range percentages {
				_, err = s.db.Exec(`
					INSERT INTO cpu_usage (
						cpu_index, user_percent, system_percent, idle_percent,
						nice_percent, iowait_percent, irq_percent, softirq_percent,
						steal_percent, guest_percent, hostname
					) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
				`, i, p.User, p.System, p.Idle, p.Nice, p.Iowait, p.Irq, p.Softirq, p.Steal, p.Guest, s.config.Hostname)
				if err != nil {
					log.Printf("Error inserting CPU usage: %v", err)
				}
			}
		}
	}
}

func (s *MonitoringService) monitorDiskUsage(ctx context.Context) {
	defer s.wg.Done()
	ticker := time.NewTicker(s.config.Intervals.DiskUsage)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			partitions, err := disk.Partitions(true)
			if err != nil {
				log.Printf("Error getting disk partitions: %v", err)
				continue
			}

			for _, partition := range partitions {
				usage, err := disk.Usage(partition.Mountpoint)
				if err != nil {
					log.Printf("Error getting disk usage for %s: %v", partition.Mountpoint, err)
					continue
				}

				_, err = s.db.Exec(`
					INSERT INTO disk_usage (
						path, total_bytes, used_bytes, free_bytes,
						usage_percent, inodes_total, inodes_used,
						inodes_free, hostname
					) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
				`, partition.Mountpoint, usage.Total, usage.Used, usage.Free,
					usage.UsedPercent, usage.InodesTotal, usage.InodesUsed,
					usage.InodesFree, s.config.Hostname)
				if err != nil {
					log.Printf("Error inserting disk usage: %v", err)
				}
			}
		}
	}
}

func (s *MonitoringService) monitorNetworkIO(ctx context.Context) {
	defer s.wg.Done()
	ticker := time.NewTicker(s.config.Intervals.NetworkIO)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			netStats, err := net.IOCounters(true)
			if err != nil {
				log.Printf("Error getting network I/O: %v", err)
				continue
			}

			for _, stat := range netStats {
				_, err = s.db.Exec(`
					INSERT INTO network_io (
						interface_name, bytes_sent, bytes_recv,
						packets_sent, packets_recv, errin, errout,
						dropin, dropout, hostname
					) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
				`, stat.Name, stat.BytesSent, stat.BytesRecv,
					stat.PacketsSent, stat.PacketsRecv, stat.Errin,
					stat.Errout, stat.Dropin, stat.Dropout, s.config.Hostname)
				if err != nil {
					log.Printf("Error inserting network I/O: %v", err)
				}
			}
		}
	}
}

func (s *MonitoringService) monitorProcessStats(ctx context.Context) {
	defer s.wg.Done()
	ticker := time.NewTicker(s.config.Intervals.ProcessStats)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			processes, err := process.Processes()
			if err != nil {
				log.Printf("Error getting processes: %v", err)
				continue
			}

			for _, p := range processes {
				name, err := p.Name()
				if err != nil {
					continue
				}

				cpuPercent, err := p.CPUPercent()
				if err != nil {
					continue
				}

				memPercent, err := p.MemoryPercent()
				if err != nil {
					continue
				}

				numThreads, err := p.NumThreads()
				if err != nil {
					continue
				}

				numFds, err := p.NumFDs()
				if err != nil {
					continue
				}

				status, err := p.Status()
				if err != nil {
					continue
				}

				_, err = s.db.Exec(`
					INSERT INTO process_stats (
						pid, name, cpu_percent, memory_percent,
						num_threads, num_fds, status, hostname
					) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
				`, p.Pid, name, cpuPercent, memPercent,
					numThreads, numFds, status, s.config.Hostname)
				if err != nil {
					log.Printf("Error inserting process stats: %v", err)
				}
			}
		}
	}
}
