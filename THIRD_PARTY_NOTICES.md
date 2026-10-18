# 第三方组件与品牌标识

项目自己的代码、文档与原创应用 Logo 使用根目录的 MIT License。
第三方组件及品牌标识的原有权利不因本项目的 MIT 许可而转让。

## 组件

- Google ML Kit 中文文字识别（`com.google.mlkit:text-recognition-chinese:16.0.1`）：由 Google 提供，遵循其原有许可和使用条款。模型由 Gradle 依赖随 APK 打包。
  文档：https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- Android SDK：由 Android Open Source Project / Google 提供，遵循相应原有许可。
- Gradle Wrapper：由 Gradle 提供，遵循其原有 Apache License 2.0 许可。

## 分类与车辆品牌标识

应用中的支付宝、微信、云闪付、QQ 音乐、ChatGPT、长安启源、小米、华为、特斯拉等标识用于辨认用户记录的类别或车辆品牌。
商标、商号及原有图形权利属于各自权利人；这些图形不因包含在本项目中而变成 MIT 授权的原创品牌。
本项目与上述品牌不存在官方关联或背书关系。

车辆品牌资源：

| 选项 | 资源 | 来源 |
| --- | --- | --- |
| 长安启源 | `app/src/main/res/drawable/brand_car_loan.png` | 项目作者提供的品牌参考图 |
| 小米 | `app/src/main/res/drawable-nodpi/brand_car_xiaomi.png` | 项目作者提供的品牌参考图，裁剪并整理透明边缘 |
| 华为 | `app/src/main/res/drawable-nodpi/brand_car_huawei.png` | 华为官网发布的花瓣标识 |
| 特斯拉 | `app/src/main/res/drawable-nodpi/brand_car_tesla.png` | 项目作者提供的品牌参考图，提取主体并整理边缘 |
| 其他 | `app/src/main/res/drawable/brand_auto.png` | 项目作者提供的通用车辆剪影 |

华为标识来源：
https://www.huawei.com/-/media/hcomponent-header/1.0.1.20260908162100/component/img/huawei_logo.png
