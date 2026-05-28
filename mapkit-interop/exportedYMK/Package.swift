
// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "YMK",
    platforms: [.iOS("16.0")],
    products: [
        .library(
            name: "YMK",
            type: .static,
            targets: ["YMK","YandexMapsMobile"])
    ],
    dependencies: [
        
    ],
    targets: [
        .target(
            name: "YMK",
            dependencies: [
                "YandexMapsMobile"
            ],
            path: "Sources"
            
            
        )
        ,.binaryTarget(name: "YandexMapsMobile", url:"https://github.com/c-villain/YandexMapsMobile/releases/download/4.17.0/YandexMapsMobile.xcframework.zip", checksum:"f21284bc1a5f9cdd36aeeaf2eb1511bcb4e67c49e46d9561341351608d019459")
    ]
)
        