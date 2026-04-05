# Frontend Dockerfile for Shortify
FROM node:20 AS build
WORKDIR /app
COPY . .
RUN npm install && npm run build

FROM nginx:1.25-alpine
COPY --from=build /app/dist/shortify-frontend /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

