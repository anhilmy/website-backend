package main

import (
	"github.com/anhilmy/website-backend/internal/status"
	"github.com/gin-gonic/gin"
)

func main() {
	router := gin.Default()

	go checkCpuUsage()
	go checkMemUsage()
	go checkNetUsage()

	status.CreateHandler(router.Group("/status"))

	router.Run(":8080")
}


func checkCpuUsage() {

}

func checkMemUsage(){

}

func checkNetUsage() {

}