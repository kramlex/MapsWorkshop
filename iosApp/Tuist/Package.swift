// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "WorkshopSample",
    dependencies: [
        .package(path: "../../mapkit-interop/exportedYMK")
    ]
)

#if TUIST
import ProjectDescription

let packageSettings = PackageSettings(
    productTypes: [
        "YMK": .staticFramework,
        "YandexMapsMobile": .staticFramework
    ]
)
#endif
