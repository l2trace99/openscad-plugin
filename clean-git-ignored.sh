#!/bin/bash

# Script to remove files from git that are now in .gitignore
# This will untrack files but keep them in your working directory

echo "This will remove tracked files that are now in .gitignore"
echo "Files will remain in your working directory but will be untracked"
echo ""
read -p "Continue? (y/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelled"
    exit 1
fi

echo "Removing ignored files from git index..."

# Remove all files from git index
git rm -r --cached .

# Re-add all files (respecting .gitignore)
git add .

echo ""
echo "Done! Files in .gitignore are now untracked."
echo "Run 'git status' to see what was removed from tracking."
echo ""
echo "To commit these changes, run:"
echo "  git commit -m 'Remove ignored files from git tracking'"
