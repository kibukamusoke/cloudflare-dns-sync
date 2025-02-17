# Contributing to CloudDNSync

Thank you for your interest in contributing to CloudDNSync! This document provides guidelines and instructions for contributing.

## Development Setup

1. Prerequisites:
   - Java 17 or later
   - Maven
   - Git

2. Clone the repository:
   ```bash
   git clone https://github.com/kibukamusoke/cloudflare-dns-sync
   cd cloudflare-dns-sync
   ```

3. Build the project:
   ```bash
   mvn clean package
   ```

## Making Changes

1. Create a new branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. Make your changes
3. Write or update tests as needed
4. Run tests:
   ```bash
   mvn test
   ```

5. Commit your changes:
   ```bash
   git add .
   git commit -m "Description of your changes"
   ```

## Pull Request Process

1. Update the README.md with details of changes if needed
2. Update the CHANGELOG.md with your changes
3. Push your changes to your fork
4. Submit a Pull Request

## Code Style

- Follow standard Java conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Keep methods focused and concise

## Reporting Issues

When reporting issues, please include:
- Description of the issue
- Steps to reproduce
- Expected behavior
- Actual behavior
- System information (OS, Java version)
- Relevant logs

## License

By contributing, you agree that your contributions will be licensed under the MIT License. 