COMPOSE_FILE ?= docker-compose.yml
DOCKER_COMPOSE ?= docker compose -f $(COMPOSE_FILE)
MVN_IMAGE ?= maven:3.9.9-eclipse-temurin-21
WORKER_MVN_DOCKER ?= docker run --rm \
	-v "$(CURDIR):/app" \
	-w /app \
	-v "$(HOME)/.m2:/root/.m2" \
	-v /var/run/docker.sock:/var/run/docker.sock \
	$(MVN_IMAGE) mvn -B -ntp -f pom.xml -pl lead-worker -am
SERVICE ?= lead-api
FILE ?=

.PHONY: up down clear reset-db enter-db build logs shell ps restart test test-unit test-integration test-docker test-unit-docker test-integration-docker

up:
	@$(DOCKER_COMPOSE) up --build

down:
	@$(DOCKER_COMPOSE) down

clear:
	@$(DOCKER_COMPOSE) down --remove-orphans

reset-db:
	@$(DOCKER_COMPOSE) down -v --remove-orphans

enter-db:
	@$(DOCKER_COMPOSE) exec postgres psql -U leaduser -d leadz

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

test:
	@$(WORKER_MVN_DOCKER) test $(if $(FILE),-Dtest=$(FILE),)

test-unit:
	@$(WORKER_MVN_DOCKER) test -DexcludeTags=integration $(if $(FILE),-Dtest=$(FILE),)

test-integration:
	@$(WORKER_MVN_DOCKER) test -DincludeTags=integration $(if $(FILE),-Dtest=$(FILE),)

test-docker:
	@$(WORKER_MVN_DOCKER) test $(if $(FILE),-Dtest=$(FILE),)

test-unit-docker:
	@$(WORKER_MVN_DOCKER) test -DexcludeTags=integration $(if $(FILE),-Dtest=$(FILE),)

test-integration-docker:
	@$(WORKER_MVN_DOCKER) test -DincludeTags=integration $(if $(FILE),-Dtest=$(FILE),)
