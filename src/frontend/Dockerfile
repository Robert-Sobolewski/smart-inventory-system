# STAGE 1 build Node + Yarn
FROM node:22-alpine AS build
WORKDIR /app
RUN corepack enable

# COPY package.json yarn.lock ./
# RUN corepack enable && yarn install --frozen-lockfile
COPY . .
RUN yarn install --frozen-lockfile
RUN yarn build

# STAGE 2 runtime
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
