export PROJECT_DIR = $(shell pwd)

-include local.mk

SNAPSHOT_VERSION := $(shell cat VERSION)
# Strip -SNAPSHOT from version
FINAL_VERSION := $(shell echo $(SNAPSHOT_VERSION) | sed 's/-SNAPSHOT//')
NEXT_VERSION := $(shell echo $(FINAL_VERSION) | awk -F. '{$$NF = $$NF + 1;} 1' | sed 's/ /./g')-SNAPSHOT
GIT_COMMIT := $(shell git rev-parse --short HEAD)

.PHONY publish-local:
publish-local:
	@echo "Preparing release..."
	./gradlew clean build
	@echo "Building release..."
	./gradlew publish
	@echo "Local Release published successfully."

.PHONY publish:
publish: publish-local
	@echo "Releasing..."
	./gradlew jreleaserFullRelease --stacktrace
	@echo "Release completed successfully."

.PHONY: ensure-git-repo-pristine
ensure-git-repo-pristine:
	@echo "Ensuring git repo is pristine"
	@[[ $(shell git status --porcelain=v1 2>/dev/null | wc -l) -gt 0 ]] && echo "Git repo is not pristine" && exit 1 || echo "Git repo is pristine"

.PHONY: bump-version
bump-version:
	@echo "Bumping version to $(NEXT_VERSION)"
	@echo $(NEXT_VERSION) > VERSION
	git add VERSION
	git commit -m "Published $(FINAL_VERSION) and prepared for $(NEXT_VERSION)"

.PHONY: tag-version
tag-version:
	@echo "Preparing release..."
	@echo "Version: $(FINAL_VERSION)"
	@echo "Commit: $(GIT_COMMIT)"
	@echo $(FINAL_VERSION) > VERSION
	git add VERSION
	git commit -m "Published $(FINAL_VERSION)"
	git tag -a $(FINAL_VERSION) -m "Release $(FINAL_VERSION)"
	git push origin $(FINAL_VERSION)
	@echo "Tag $(FINAL_VERSION) created and pushed to origin"

.PHONY: release
release: ensure-git-repo-pristine tag-version bump-version
	git push
	@echo "Release $(VERSION) completed and pushed to origin"
