取至 : https://github.com/zszdevelop/java-file-server

# java-file-server
## 1. Java 文件服务

java常见文件服务实现，封装统一常用 *RESTful* *API*，针对各文件服务特性提供特性service。

### 1.1 支持文件服务类型

- 本地存储
- Minio
- FastDFS（开发中）
- 阿里云存储（开发中）

### 1.2 文件 *RESTful* API

- 文件上传

  ```
  POST http://localhost:9310/file/upload
  ```

- 文件下载

  ```
  GET http://localhost:9310/file/download?fileName=2021/12/12/test.png
  ```

- 预览文件（仅支持图片和Pdf）

  ```
  GET http://localhost:9310/file/preview?fileName=2021/12/12/test.png
  ```

- 获取预览地址

  ```
  GET http://localhost:9310/file/getPreviewUrl?fileName=2021/12/12/test.png
  ```

  - Minio 实现可支持设置过期时间

- 文件是否存在

  ```
  GET http://localhost:9310/file/exists?fileName=2021/12/12/test.png
  ```

- 删除文件

  ```
  DELETE http://localhost:9310/file/delete?fileName=2021/12/12/test.png
  ```



## 2. 实现思路

1. 文件controller

   提供统一的controller层，通过service接口调用不同的文件服务实现

2. 文件service

   文件服务实现有很多种（本地存储、Minio、云存储等），每种类型除了支持正常上传、下载、预览、删除外。还提供了一些其他特性，并不希望我们的文件服务变得只支持通用API，对于平台特性的也能够支持。

   ```
   ├── service            								 // service层
   │   └── FileService                    // 通用文件服务接口
   |		└── LocalFileService               // 本地存储扩展接口-继承通用文件服务
   |		└── MinioFileService               // minio扩展接口-继承通用文件服务
   |		└──	impl
   |				└── LocalFileServiceImpl       // 本地存储实现
   |				└── MinioFileServiceImpl       // minio实现
   ```

3. 返回结果封装

  - 各平台文件服务返回统一的结果，结果带有泛型支持，方便获取数据。

    ```java
    public class FileResult<T> {
        // 错误码
        private Integer code;
        // 消息
        private String msg;
        // 返回结果
        private T data;
    }
    ```

  - 各平台的有特性返回，则通过FileInfo 继承 HashMap 进行扩展

    ```java
    public class FileInfo extends HashMap<String, Object> {
    		...
    }
    ```


4. 文件路径与重命名

   - 文件路径与重命名都在外部控制

   - 所有的service传的文件名，都是包含 `文件的路径`与`文件名（开发者决定是否重命名）`，方便开发者们制定自定义

   - 默认的文件名处理(日期+uuid)

     ```java
      /**
          * 获取包含路径的文件名
          * TODO 此处根据项目实际情况处理，制定存储路径和重命名文件
          * @param file
          * @return
          */
         private String getNewPathFilename(MultipartFile file) {
             Date now = new Date();
             SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
             String datePath =  sdf.format(now);
             String extension = FileUtil.extName(file.getOriginalFilename());
     
             String pathFilename = datePath + File.separator + IdUtil.fastSimpleUUID() + "." + extension;
             return pathFilename;
         }
     ```



## 3. 測試

無限網速，client 單線程
![image](https://github.com/user-attachments/assets/f1de8e0a-1edf-48a5-93f3-cdf4871f9044)
服務端監控
![image](https://github.com/user-attachments/assets/e1f6770f-a2d4-474f-804f-5ea5bfbb2405)


無限網速，client 2線程
![image](https://github.com/user-attachments/assets/8e19ed1f-8677-4a8b-8f69-6472779bb2d5)
服務端監控
![image](https://github.com/user-attachments/assets/56d989a4-6de9-464e-92aa-4c5e9d972e8f)


無限網速，client 5線程
![image](https://github.com/user-attachments/assets/4855c690-ff15-4422-b834-3c6d23dc7746)
服務端監控
![image](https://github.com/user-attachments/assets/9115ce90-09c6-4a23-9434-b27015d97198)


無限網速，client 單線程 filechannel 方法
​![image](https://github.com/user-attachments/assets/85bd8032-59dc-46d9-9e3a-45fa779dcbce)
服務端監控
![image](https://github.com/user-attachments/assets/750bfd8f-3f4f-41eb-bd78-e0f134c46d20)


無限網速，client 2線程 filechannel 方法
![image](https://github.com/user-attachments/assets/ac81cb6c-bcd9-43ae-9ad4-0aafc095b9c5)
服務端監控
![image](https://github.com/user-attachments/assets/27371d4e-cb24-4c79-a9f6-6893cba87cb2)


無限網速，client 5線程 filechannel 方法
![image](https://github.com/user-attachments/assets/66600a0f-fd7d-42ba-a94a-dd7c0b4da5df)
服務端監控
![image](https://github.com/user-attachments/assets/2ca1bb82-ab83-430a-9ab2-80e8cfdcf75e)


限網速5M，client 單線程
![image](https://github.com/user-attachments/assets/ca983c70-6219-481a-99dd-4b32b64d5e75)
服務端監控
![image](https://github.com/user-attachments/assets/2540ab76-2635-4102-964c-d3434fed9b2a)

限網速5M，client 2線程
![image](https://github.com/user-attachments/assets/eb60984b-5e33-451c-8bf9-3ffc2da1c3de)
服務端監控
![image](https://github.com/user-attachments/assets/7502369b-f83d-4a84-bb17-f6ce31ef629d)

限網速5M，client 5線程
![image](https://github.com/user-attachments/assets/89aaf469-9b71-48e4-a8c4-b217bf5e88d2)
服務端監控
![image](https://github.com/user-attachments/assets/93576c55-be50-4f15-a965-d34380e09ab5)

限網速5M，client 10線程
![image](https://github.com/user-attachments/assets/9f31ea37-83f3-45c5-a480-6d60c6efc545)
![image](https://github.com/user-attachments/assets/10710ebf-3cf7-4ac6-9055-22675e60384e)

服務端監控
![image](https://github.com/user-attachments/assets/bff3cf4f-0876-482a-ba5f-7df234ea785b)



限網速10M，client 1線程
![image](https://github.com/user-attachments/assets/1891c5b9-47c3-4e54-82dc-bb6695ffa643)
服務端監控
![image](https://github.com/user-attachments/assets/3b27f303-bd13-4aa4-a6ee-8cf554fc1ca0)

限網速10M，client 2線程
![image](https://github.com/user-attachments/assets/2a992b5e-5937-44a0-a9f8-befb7af3c4ad)
服務端監控
![image](https://github.com/user-attachments/assets/5a932e47-a7a4-453b-8000-61340626df01)


限網速10M，client 5線程
![image](https://github.com/user-attachments/assets/842e8db3-d90b-4c86-9644-6007f4324c3b)
服務端監控
![image](https://github.com/user-attachments/assets/e2ff4ac9-a565-4ece-92b3-de871b6b8414)

限網速10M，client 10線程
![image](https://github.com/user-attachments/assets/7c0e2d36-be6a-491e-8fca-ef9dbdbea92b)
服務端監控
![image](https://github.com/user-attachments/assets/380be47c-8196-4ea9-a6e4-facd214ab4e0)

限網速20M，client 1線程
![image](https://github.com/user-attachments/assets/3d6beb2a-72e8-46af-926a-0fead8e8ccb2)
服務端監控
![image](https://github.com/user-attachments/assets/edee1f98-8e52-4a02-82e2-820b8c9ee329)

限網速20M，client 2線程
![image](https://github.com/user-attachments/assets/144e4616-3988-414d-9eb7-3afd31e09fb8)
服務端監控
![image](https://github.com/user-attachments/assets/6e56775f-e75b-4e63-824b-cd7701a8a374)

限網速20M，client 5線程
![image](https://github.com/user-attachments/assets/ae06e73c-8ecb-49cd-a22f-5061ade24969)
服務端監控
![image](https://github.com/user-attachments/assets/366dba8b-6c47-4d83-8313-e1c2339a3f95)

限網速20M，client 10線程
![image](https://github.com/user-attachments/assets/6b6d5de2-7932-4847-8260-cb57659220b0)
服務端監控
![image](https://github.com/user-attachments/assets/72ac77bc-1952-41a7-98ee-37ce551a4d98)
