阿里云 OSS 配置（必做）
本项目使用阿里云 OSS 进行图片存储。为了安全起见，敏感信息未硬编码在项目中。运行前请按照以下步骤配置您的密钥：
登录 [阿里云控制台](https://oss.console.aliyun.com/)，获取您的 `AccessKey ID` 和 `AccessKey Secret`。

**配置本地环境变量**
1. 搜索“环境变量” -> 选择编辑系统环境变量 -> 点击环境变量按钮。
2. 在“用户变量”或“系统变量”中，点击新建。
3. 变量名输入：ALIOSS_ACCESS_KEY_ID
   变量值输入：实际 ID
4. 变量名：ALIOSS_ACCESS_KEY_SECRET
   变量值：实际 Secret

**配置IDEA**
1. 打开 IDEA，点击右上角的 **Edit Configurations...**。
2. 找到本项目的 Spring Boot 启动类，在 **Environment variables** 中添加以下两个环境变量：
   ```text
   ALI_OSS_ACCESS_KEY_ID=你的真实AccessKeyID;ALI_OSS_ACCESS_KEY_SECRET=你的真实AccessKeySecret
