package main

import (
	"fmt"
	"log"
	"time"

	"github.com/anhilmy/website-backend/services/status/internal/status"
	"github.com/gin-gonic/gin"
	"github.com/shirou/gopsutil/v4/cpu"
	"github.com/shirou/gopsutil/v4/mem"
	"github.com/shirou/gopsutil/v4/net"
	"github.com/shirou/gopsutil/v4/sensors"
)

func main() {

	router := gin.Default()

	// go checkCpuUsage(fileLog, errLog)
	// go checkMemUsage(fileLog, errLog)
	// go checkNetUsage(fileLog, errLog)
	// go checkTemperature(fileLog, errLog)

	status.CreateHandler(router.Group("/status"))

	router.Run(":8080")
}

func checkCpuUsage(log *log.Logger, errLog *log.Logger) {
	for {
		cpuPercent, err := cpu.Percent(0, false)
		if err != nil {
			errLog.Fatalln(err)
		}
		log.Print(cpuPercent)

		time.Sleep(time.Minute)
	}
}

func checkMemUsage(log *log.Logger, errLog *log.Logger) {
	for {
		vMem, err := mem.VirtualMemory()
		if err != nil {
			errLog.Fatalln(err)
		}

		log.Println(vMem.Used)

		time.Sleep(time.Minute)
	}

}

func checkNetUsage(log *log.Logger, errLog *log.Logger) {
	for {
		netStat, err := net.IOCounters(false)
		if err != nil {
			errLog.Fatalln(err)
		}

		log.Println(netStat[0].BytesSent, "/", netStat[0].BytesRecv)

		time.Sleep(time.Minute)
	}
}

func checkTemperature(log *log.Logger, errLog *log.Logger) {
	for {
		hostInfo, err := sensors.SensorsTemperatures()
		var cpuTemp float64
		if err != nil {
			errLog.Fatalln(err)
		}

		for _, sensor := range hostInfo {
			fmt.Println(sensor.SensorKey)
			if sensor.SensorKey == "cpu" { // Change this depending on platform
				cpuTemp = sensor.Temperature
				break
			}
		}

		log.Println(cpuTemp)

		time.Sleep(time.Minute)
	}
}
