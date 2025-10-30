#!/usr/bin/env python3
import re

# Read the project file
with open('iosApp/iosApp.xcodeproj/project.pbxproj', 'r') as f:
    lines = f.readlines()

# IDs to remove (the duplicates I added)
ids_to_remove = [
    '3872D760C191414F9ED53AD4',  # FirebaseBridgeProtocol.swift
    '89C5D05BBB264883B616337C',  # FirebaseBridgeProvider.swift
    'F5C2A307D0DD41F89F19139F',  # FirebaseBridgeWrapper.swift
    'B19A93823BA74116AF7FD0FD',  # FirebaseBridgeInitializer.swift
    '199DCCE303B4441E81C508E1',  # FirebaseIOSBridge.swift (duplicate)
    '182EDC837F044498BD1A7EAA',  # FirebaseIOSBridge.h
    'F005C9A22FD34106A1F74B90',  # Build file refs
    'C03E3A1194D64E47B514B273',
    '37EA28CDC6C54FDB8F426461',
    'AA1260CF7BF04ACAB5917FC7',
    '504820B28EAB4B838B3EA1F4',
]

# Filter out lines containing these IDs
filtered_lines = []
for line in lines:
    should_keep = True
    for id_to_remove in ids_to_remove:
        if id_to_remove in line:
            should_keep = False
            break
    if should_keep:
        filtered_lines.append(line)

# Write back
with open('iosApp/iosApp.xcodeproj/project.pbxproj', 'w') as f:
    f.writelines(filtered_lines)

print("Cleaned up duplicate entries")
