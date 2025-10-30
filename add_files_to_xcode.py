#!/usr/bin/env python3
import re
import uuid

# Read the project file
with open('iosApp/iosApp.xcodeproj/project.pbxproj', 'r') as f:
    content = f.read()

# Files to add
files_to_add = [
    'FirebaseBridgeProtocol.swift',
    'FirebaseBridgeProvider.swift',
    'FirebaseBridgeWrapper.swift',
    'FirebaseBridgeInitializer.swift',
    'FirebaseIOSBridge.swift',
    'FirebaseIOSBridge.h'
]

# Generate UUIDs for each file (2 per file: one for PBXFileReference, one for PBXBuildFile)
file_refs = {}
build_files = {}
for filename in files_to_add:
    file_refs[filename] = uuid.uuid4().hex[:24].upper()
    build_files[filename] = uuid.uuid4().hex[:24].upper()

# Find the Services group section
services_pattern = r'(/\* Services \*/.*?children = \()(.*?)(\);)'
match = re.search(services_pattern, content, re.DOTALL)

if match:
    # Add file references to Services group
    services_children = match.group(2)
    for filename in files_to_add:
        file_ref_line = f"\n\t\t\t\t{file_refs[filename]} /* {filename} */,"
        if file_ref_line not in services_children:
            services_children += file_ref_line
    
    content = content[:match.start(2)] + services_children + content[match.end(2):]

# Find PBXBuildFile section
build_file_section_pattern = r'(/\* Begin PBXBuildFile section \*/)(.*?)(/\* End PBXBuildFile section \*/)'
match = re.search(build_file_section_pattern, content, re.DOTALL)

if match:
    build_file_section = match.group(2)
    for filename in files_to_add:
        if filename.endswith('.swift'):
            build_file_entry = f"\n\t\t{build_files[filename]} /* {filename} in Sources */ = {{isa = PBXBuildFile; fileRef = {file_refs[filename]} /* {filename} */; }};"
            if build_file_entry not in build_file_section:
                build_file_section += build_file_entry
    
    content = content[:match.start(2)] + build_file_section + content[match.end(2):]

# Find PBXFileReference section
file_ref_section_pattern = r'(/\* Begin PBXFileReference section \*/)(.*?)(/\* End PBXFileReference section \*/)'
match = re.search(file_ref_section_pattern, content, re.DOTALL)

if match:
    file_ref_section = match.group(2)
    for filename in files_to_add:
        if filename.endswith('.swift'):
            file_type = 'sourcecode.swift'
        else:
            file_type = 'sourcecode.c.h'
        file_ref_entry = f"\n\t\t{file_refs[filename]} /* {filename} */ = {{isa = PBXFileReference; lastKnownFileType = {file_type}; name = {filename}; path = Services/{filename}; sourceTree = \"<group>\"; }};"
        if file_ref_entry not in file_ref_section:
            file_ref_section += file_ref_entry
    
    content = content[:match.start(2)] + file_ref_section + content[match.end(2):]

# Find PBXSourcesBuildPhase section and add to Sources
sources_pattern = r'(/\* Sources \*/.*?files = \()(.*?)(\);)'
match = re.search(sources_pattern, content, re.DOTALL)

if match:
    sources_files = match.group(2)
    for filename in files_to_add:
        if filename.endswith('.swift'):
            source_line = f"\n\t\t\t\t{build_files[filename]} /* {filename} in Sources */,"
            if source_line not in sources_files:
                sources_files += source_line
    
    content = content[:match.start(2)] + sources_files + content[match.end(2):]

# Write back
with open('iosApp/iosApp.xcodeproj/project.pbxproj', 'w') as f:
    f.write(content)

print("Successfully added files to Xcode project")
for filename in files_to_add:
    print(f"  - {filename}")
