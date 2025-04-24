package internal

import (
	"context"
)

type Service interface {
	Login(ctx context.Context, req LoginRequest) (LoginResponse, error)
	LoginGuest(ctx context.Context, req LoginGuestRequest) (LoginResponse, error)
}

type service struct {
	repository Repository
}

// Login implements Service.
func (service) Login(ctx context.Context, req LoginRequest) (LoginResponse, error) {
	panic("unimplemented")
}

// LoginGuest implements Service.
func (service) LoginGuest(ctx context.Context, req LoginGuestRequest) (LoginResponse, error) {
	panic("unimplemented")
}

func NewService() Service {
	return service{}
}
