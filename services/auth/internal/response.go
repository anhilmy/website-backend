package internal

type Response struct {
	Code     int
	Messsage string
	Data     interface{}
}

type LoginResponse struct {
	AccessToken  string
	RefreshToken string
}
