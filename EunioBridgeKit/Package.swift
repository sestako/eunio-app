// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "EunioBridgeKit",
    platforms: [
        .iOS(.v15)
    ],
    products: [
        .library(
            name: "EunioBridgeKit",
            targets: ["EunioBridgeKit"]
        ),
    ],
    targets: [
        .target(
            name: "EunioBridgeKit",
            dependencies: []
        )
    ]
)
