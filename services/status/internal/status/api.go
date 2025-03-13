package status

import "github.com/gin-gonic/gin"

func CreateHandler(rg *gin.RouterGroup) {
	rg.GET("/cpu", getCpuData)
	rg.GET("/mem", getMemData)
	rg.GET("/net", getNetworkData)
}

func getCpuData(c *gin.Context) {

}

func getMemData(c *gin.Context) {

}

func getNetworkData(c *gin.Context) {

}
