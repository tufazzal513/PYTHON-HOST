package com.python.localhost.project

import java.io.File

/**
 * Generates starter files for the supported v1 templates. No API keys or tokens are
 * ever hardcoded — secret values are read from a .env file at runtime.
 */
object ProjectTemplates {

    fun apply(template: String, dir: File, projectName: String) {
        when (template) {
            "flask" -> flask(dir, projectName)
            "fastapi" -> fastapi(dir, projectName)
            "telegram" -> telegram(dir, projectName)
            "automation" -> automation(dir, projectName)
            else -> basic(dir, projectName)
        }
        writeReadme(dir, projectName, template)
        writeGitignore(dir)
    }

    private fun write(file: File, name: String, content: String) {
        File(file, name).writeText(content)
    }

    private fun basic(dir: File, name: String) {
        write(
            dir, "main.py", """
# $name
# Created with PyMobile IDE

def main():
    print("Hello from $name!")
    for i in range(5):
        print(f"count: {i}")

if __name__ == "__main__":
    main()
        """.trimIndent()
        )
        write(dir, "requirements.txt", "")
    }

    private fun flask(dir: File, name: String) {
        write(
            dir, "main.py", """
from flask import Flask

app = Flask(__name__)

@app.route("/")
def index():
    return "Hello from $name (Flask)!"

if __name__ == "__main__":
    # Bind to 0.0.0.0 so the LAN URL is reachable on your network.
    app.run(host="0.0.0.0", port=5000)
        """.trimIndent()
        )
        write(dir, "requirements.txt", "flask\n")
    }

    private fun fastapi(dir: File, name: String) {
        write(
            dir, "main.py", """
from fastapi import FastAPI

app = FastAPI()

@app.get("/")
def read_root():
    return {"message": "Hello from $name (FastAPI)!"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
        """.trimIndent()
        )
        write(dir, "requirements.txt", "fastapi\nuvicorn\n")
    }

    private fun telegram(dir: File, name: String) {
        write(
            dir, "main.py", """
# Telegram bot template (python-telegram-bot).
# Add your token to a .env file as TELEGRAM_BOT_TOKEN and never commit it.
import os
from dotenv import load_dotenv

load_dotenv()

def main():
    token = os.environ.get("TELEGRAM_BOT_TOKEN")
    if not token:
        raise RuntimeError("Set TELEGRAM_BOT_TOKEN in your .env file")
    # from telegram.ext import ApplicationBuilder, CommandHandler
    # app = ApplicationBuilder().token(token).build()
    # app.run_polling()
    print("Bot token loaded. Install python-telegram-bot and uncomment the runner.")

if __name__ == "__main__":
    main()
        """.trimIndent()
        )
        write(dir, "requirements.txt", "python-telegram-bot\npython-dotenv\n")
        write(dir, ".env", "TELEGRAM_BOT_TOKEN=\n")
    }

    private fun automation(dir: File, name: String) {
        write(
            dir, "main.py", """
# Automation script template.
import time

def main():
    print("Automation started: $name")
    while True:
        print("working...")
        time.sleep(10)

if __name__ == "__main__":
    main()
        """.trimIndent()
        )
        write(dir, "requirements.txt", "")
    }

    private fun writeReadme(dir: File, name: String, template: String) {
        write(dir, "README.md", "# $name\n\nTemplate: $template\n\nCreated with PyMobile IDE.\n")
    }

    private fun writeGitignore(dir: File) {
        write(dir, ".gitignore", "__pycache__/\n*.pyc\n.pymobile/\n.env\n")
    }
}
