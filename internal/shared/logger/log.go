package logger

import (
	"os"

	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

type Logger interface {
	Defer() error
	Info(msg ...string)
	Warn(msg ...string)
	Error(msg ...string)
}

type logger struct {
	logFile *os.File
	Log     *zap.SugaredLogger
}

func NewLogger(logPath string, errPath string) (log Logger, err error) {
	logFile, err := os.OpenFile("http.log", os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0664)
	if err != nil {
		return
	}

	writeSyncer := zapcore.AddSync(logFile)
	encodeConfig := zap.NewProductionEncoderConfig()
	encodeConfig.EncodeTime = zapcore.ISO8601TimeEncoder
	core := zapcore.NewCore(zapcore.NewJSONEncoder(encodeConfig), writeSyncer, zap.InfoLevel)
	zapLog := zap.New(core).Sugar()

	log = &logger{
		logFile: logFile,
		Log:     zapLog,
	}

	return
}

func (l *logger) Defer() (err error) {
	err = l.logFile.Close()
	if err != nil {
		return
	}

	err = l.Log.Sync()
	if err != nil {
		return
	}

	return
}

func (l *logger) Info(msg ...string) {
	l.Log.Info(msg)
}

func (l *logger) Warn(msg ...string) {
	l.Log.Warn(msg)
}

func (l *logger) Error(msg ...string) {
	l.Log.Error(msg)
}
