package internal

type LoginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type LoginGuestRequest struct {
	Username string `json:"username"`
}