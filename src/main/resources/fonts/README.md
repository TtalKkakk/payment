# 한글 PDF 폰트

영수증 PDF에서 한글이 정상 표시되려면 이 디렉터리에 한글 지원 TTF 폰트를 넣어야 합니다.

1. [Google Fonts - Noto Sans KR](https://fonts.google.com/noto/specimen/Noto+Sans+KR) 접속
2. "Download family" → ZIP 다운로드 후 압축 해제
3. `NotoSansKR-Regular.ttf` 파일을 이 디렉터리(`src/main/resources/fonts/`)에 복사

파일이 없으면 PDF는 생성되지만 한글이 깨져 보일 수 있습니다.
