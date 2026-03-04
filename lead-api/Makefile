APP_NAME ?= lead-api
DOCKER_COMPOSE ?= docker compose

.PHONY: build up down logs shell

build:
	@docker build -t $(APP_NAME) .

up:
	@$(DOCKER_COMPOSE) up --build

down:
	@$(DOCKER_COMPOSE) down

logs:
	@$(DOCKER_COMPOSE) logs -f app

shell:
	@$(DOCKER_COMPOSE) exec app /bin/sh
