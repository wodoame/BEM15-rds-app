#!/usr/bin/env bash
# Fetch CloudFormation outputs from the deployed todo-app stack and sync them
# as GitHub Actions variables and secrets for wodoame/BEM15-rds-app.
# Run this after any infrastructure deployment to keep GitHub in sync.
#
# Prerequisites: AWS CLI, gh CLI (authenticated as wodoame), jq
set -euo pipefail

STACK_NAME="todo-app-stack"
AWS_REGION="us-east-1"
REPO="wodoame/BEM15-rds-app"
ENV="production"

# ── helpers ───────────────────────────────────────────────────────────────────

cfn_output() {
  local stack=$1 key=$2
  aws cloudformation describe-stacks \
    --stack-name "$stack" \
    --region "$AWS_REGION" \
    --query "Stacks[0].Outputs[?OutputKey=='$key'].OutputValue" \
    --output text
}

child_stack() {
  local logical=$1
  aws cloudformation list-stack-resources \
    --stack-name "$STACK_NAME" \
    --region "$AWS_REGION" \
    --query "StackResourceSummaries[?LogicalResourceId=='${logical}'].PhysicalResourceId" \
    --output text
}

# ── fetch values from CloudFormation ─────────────────────────────────────────

echo "Fetching CloudFormation outputs from '$STACK_NAME'..."

# Root stack outputs
ECR_REPO_URI=$(cfn_output "$STACK_NAME" ECRRepositoryUri)
ARTIFACT_BUCKET=$(cfn_output "$STACK_NAME" ArtifactBucketName)
DB_HOST=$(cfn_output "$STACK_NAME" RDSProxyEndpoint)
REDIS_HOST=$(cfn_output "$STACK_NAME" RedisEndpoint)

# Child stack outputs
IAM_STACK=$(child_stack IAMStack)
DB_STACK=$(child_stack DatabaseStack)

ECS_TASK_EXECUTION_ROLE_ARN=$(cfn_output "$IAM_STACK" ECSTaskExecutionRoleArn)
ECS_TASK_ROLE_ARN=$(cfn_output "$IAM_STACK" ECSTaskRoleArn)
GITHUB_ACTIONS_ROLE_ARN=$(cfn_output "$IAM_STACK" GitHubActionsRoleArn)

DB_NAME=$(cfn_output "$DB_STACK" DBName)
DB_USERNAME=$(cfn_output "$DB_STACK" DBUsername)

# Derived values
ECR_REPOSITORY="${ECR_REPO_URI##*/}"   # strip registry prefix (keep only repo name)
TASK_FAMILY="${ENV}-todo-app"
LOG_GROUP="/ecs/${ENV}-todo-app"

# ── sync to GitHub ────────────────────────────────────────────────────────────

echo "Syncing to GitHub repo '$REPO'..."

# Variables (non-sensitive — visible in workflow logs)
gh variable set AWS_REGION      --body "$AWS_REGION"      --repo "$REPO"
gh variable set ECR_REPOSITORY  --body "$ECR_REPOSITORY"  --repo "$REPO"
gh variable set ARTIFACT_BUCKET --body "$ARTIFACT_BUCKET" --repo "$REPO"
gh variable set DB_HOST         --body "$DB_HOST"         --repo "$REPO"
gh variable set DB_NAME         --body "$DB_NAME"         --repo "$REPO"
gh variable set DB_USERNAME     --body "$DB_USERNAME"     --repo "$REPO"
gh variable set REDIS_HOST      --body "$REDIS_HOST"      --repo "$REPO"
gh variable set TASK_FAMILY     --body "$TASK_FAMILY"     --repo "$REPO"
gh variable set LOG_GROUP       --body "$LOG_GROUP"       --repo "$REPO"

# Secrets (contain account IDs or are credentials)
gh secret set AWS_ROLE_ARN                --body "$GITHUB_ACTIONS_ROLE_ARN"    --repo "$REPO"
gh secret set ECS_TASK_EXECUTION_ROLE_ARN --body "$ECS_TASK_EXECUTION_ROLE_ARN" --repo "$REPO"
gh secret set ECS_TASK_ROLE_ARN           --body "$ECS_TASK_ROLE_ARN"           --repo "$REPO"

echo "Done. All variables and secrets are up to date."
