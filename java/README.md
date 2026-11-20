# ����α�� �� ��¼ע����Ŀ������������

> ���ļ��ǡ�������������¼����д��δ�����Լ� / AI ���ֿ�������ÿ������������
> һ�仰��**˫�� `start-all.bat` ����**�����඼�������������¡�

## һ�仰˵��
������α���������ң�����Ϊ������ĵ�¼ע����վ��
ǰ�� Vue3 + Vite����� Spring Boot 3.2.5 + MyBatis-Plus + MySQL��

## Ŀ¼�ṹ
```
java/
������ start-all.bat        �� һ�����������ܼ�⻷����˫�����ɣ�
������ stop-all.bat         �� һ��ֹͣ�����˿� 5173/5174/8080 ����
������ false-god-backend/   �� ��ˣ�Spring Boot 3.2.5 + MyBatis-Plus + MySQL��
��   ������ sql/false_god.sql   �� ����ű����������˺ţ�
��   ������ pom.xml
������ html/                �� ǰ�ˣ�Vue3 + Vite��
    ������ src/api/auth.js     �� ��¼/ע��ӿ�
    ������ src/api/session.js  �� ��¼̬���ش洢��������
    ������ src/components/ThroneCard.vue �� ��¼����λҳ��������
    ������ package.json
    ������ vite.config.js
```

## �ؼ���Ϣ�����û�׼��
| ��Ŀ | ֵ |
|------|-----|
| ���� IP | `192.168.68.63`����̫���� |
| ��˶˿� | 8080 |
| ǰ�˶˿� | 5173 |
| ǰ�˴��� | `/api` �� `http://192.168.68.63:8080` |
| ���ݿ� | MySQL `false_god`��`192.168.68.63:3306`��root / 123456 |
| �������� | http://192.168.68.63:5173 |
| ���������� | ͬһ WiFi ���ֻ�/�������Դ� http://192.168.68.63:5173 |
| �����˺� | chenhaoyu / 123456 |
| ��˽ӿ� | `POST /api/login`��`POST /api/register` |
| Java | Zulu 21��`C:\Program Files\Zulu\zulu-21`�� |
| Maven | `~/.m2/wrapper/dists/apache-maven-3.9.9-bin/<hash>/apache-maven-3.9.9` |

## �������裨�Ƽ���
**˫�� `start-all.bat`**���ű����Զ���� 6 ����
1. �Զ����� Java��JAVA_HOME �� Zulu 21 �� jdk-17 �� PATH����·���ˣ�
2. ����ռ�õ� 5173 / 8080 �˿ڣ��������������ʧ�ܵĸ���
3. ��� MySQL �Ƿ����У�û�ܾͳ��� `net start MySQL80` ����
4. ��� `false_god` �⣻���������Զ����Ⲣ���� `sql/false_god.sql`
5. ȱ jar �� Maven ������Ȼ��������ˣ�**�������ͨ����ż���**
6. ȱ `node_modules` �� `npm install`������ǰ�ˣ�����Զ��������

������ɺ����̨���ӡ��
- ǰ�� http://192.168.68.63:5173
- ��� http://192.168.68.63:8080
- �����˺� chenhaoyu / 123456

## ֹͣ����
˫�� `stop-all.bat`�����˿ڣ�5173 / 5174 / 8080��������Ӧ���̡�

## ����ע��
- **Maven ����ϵͳ PATH**���� Maven Wrapper �����·�������㣺
  `~/.m2/wrapper/dists/apache-maven-X-bin/<hash>/apache-maven-X/bin/mvn.cmd`
  `start-all.bat` �Ѱ�����ṹ�Զ����ң�����������Ľű���
- `.bat` �ļ��� **GBK ����**����Ҫ�� `chcp 65001`���������Ļ����롣

## ��������
- **ҳ����� / �ӿ�����Ӧ**������Ǿɽ���ռ�� 5173/8080 ���Ѹ�����ռ�˿ڵ����ɻVite ������ 5174����
  `start-all.bat` ����ʱ���Զ�����Ҳ����˫�� `stop-all.bat`��
- **��¼�� 500 / Unknown database 'false_god'**�����ݿ�û�����ű����Զ������ֶ��޸���
  ```
  mysql -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS false_god DEFAULT CHARACTER SET utf8mb4;"
  mysql -uroot -p123456 --default-character-set=utf8mb4 false_god < sql/false_god.sql
  ```
  ע��**����� `--default-character-set=utf8mb4`**���������ı� `ERROR 1366`��
- **ǰ�������Ϻ��**��ȷ�� 8080 ����������� `vite.config.js` ���� target �Ƿ�Ϊ `192.168.68.63:8080`��
- **�ֻ��򲻿�ҳ��**���� �ֻ��������ͬһ WiFi���� Windows ����ǽ���� 5173/8080 ��վ���״ο���ʱ�رշ���ǽ��֤������ ȷ�� IP û�䣨����/����·�ɻ�䣬`ipconfig` �飩��
- **����ʱ�� Mock ģʽ**���� `html/src/api/auth.js` �� `USE_MOCK` �ĳ� `true`��

## ��֪�����ƣ��´ο�����
1. **token �Ǽٵ�**����˷��� `fake-jwt-<ʱ���>`��û����ʵǩ����У�飬Ҳû�м�Ȩ��������
2. **�޼�Ȩ�ӿ�**��ȱ�� `/api/me` ֮��� el �ӿڣ�ǰ��ˢ��ֻ�ܿ� localStorage �ָ���¼̬��
3. **����δ���ι�ϣ**���� `common/PasswordUtil.java`����ȷ���Ƿ����� BCrypt/MD5+�Ρ�
4. **�޺�˵�Ԫ����**��`src/test` Ŀ¼�����ڡ�
5. **��ǰ��·��**����ҳ״̬�л���`view` ref����δ���� vue-router��ˢ�»�ص�¼ҳ��
6. �������� **CORS ����**��`config/CorsConfig.java`�����ս���Ŀǰ��������ȫ����Դ��
