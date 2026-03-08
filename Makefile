COMPOSE_FILE ?= docker-compose.yml
DOCKER_COMPOSE ?= docker compose -f $(COMPOSE_FILE)
SERVICE ?= lead-api

.PHONY: up down build logs shell ps restart

up:
	@$(DOCKER_COMPOSE) up --build

down:
	@$(DOCKER_COMPOSE) down

build:
	@$(DOCKER_COMPOSE) build

logs:
	@$(DOCKER_COMPOSE) logs -f $(SERVICE)

shell:
	@$(DOCKER_COMPOSE) exec $(SERVICE) /bin/sh

ps:
	@$(DOCKER_COMPOSE) ps

restart:
	@$(DOCKER_COMPOSE) restart $(SERVICE)
