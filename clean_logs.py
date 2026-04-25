import os
import re
import gzip
from datetime import datetime
from collections import defaultdict

# 日志文件目录
logs_dir = r'c:\Users\zanboring\OneDrive - Ormesby Primary\Desktop\Recruitment 2\Recruitment\recruitment-system\logs'

# 日志格式正则表达式
log_pattern = re.compile(r'^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) \[(.*?)\] (DEBUG|INFO|WARN|ERROR|FATAL) (.*?) - (.*)$')

# 清洗统计
cleaning_stats = {
    'total_files': 0,
    'total_records': 0,
    'invalid_records': 0,
    'duplicate_records': 0,
    'processed_records': 0,
    'file_stats': defaultdict(lambda: {'input': 0, 'output': 0, 'invalid': 0, 'duplicates': 0})
}

# 重复记录检测
duplicate_check = set()

# 清洗规则
valid_log_levels = {'DEBUG', 'INFO', 'WARN', 'ERROR', 'FATAL'}

# 标准化时间戳格式
def normalize_timestamp(timestamp):
    try:
        # 解析时间戳
        dt = datetime.strptime(timestamp, '%Y-%m-%d %H:%M:%S.%f')
        # 格式化为标准格式
        return dt.strftime('%Y-%m-%d %H:%M:%S')
    except:
        return None

# 验证日志级别
def validate_log_level(level):
    return level in valid_log_levels

# 处理单条日志记录
def process_log_line(line):
    global cleaning_stats
    
    # 去除首尾空白
    line = line.strip()
    if not line:
        cleaning_stats['invalid_records'] += 1
        return None
    
    # 匹配日志格式
    match = log_pattern.match(line)
    if not match:
        cleaning_stats['invalid_records'] += 1
        return None
    
    timestamp, thread, level, logger, message = match.groups()
    
    # 标准化时间戳
    normalized_timestamp = normalize_timestamp(timestamp)
    if not normalized_timestamp:
        cleaning_stats['invalid_records'] += 1
        return None
    
    # 验证日志级别
    if not validate_log_level(level):
        cleaning_stats['invalid_records'] += 1
        return None
    
    # 检测重复记录
    record_hash = f"{normalized_timestamp}|{thread}|{level}|{logger}|{message}"
    if record_hash in duplicate_check:
        cleaning_stats['duplicate_records'] += 1
        return None
    duplicate_check.add(record_hash)
    
    # 构建清洗后的日志记录
    cleaned_line = f"{normalized_timestamp} [{thread}] {level} {logger} - {message}"
    cleaning_stats['processed_records'] += 1
    return cleaned_line

# 处理单个文件
def process_file(input_file, output_file):
    global cleaning_stats
    
    file_name = os.path.basename(input_file)
    cleaning_stats['total_files'] += 1
    
    try:
        with open(input_file, 'r', encoding='utf-8', errors='ignore') as f:
            lines = f.readlines()
        
        cleaning_stats['total_records'] += len(lines)
        cleaning_stats['file_stats'][file_name]['input'] = len(lines)
        
        cleaned_lines = []
        for line in lines:
            cleaned_line = process_log_line(line)
            if cleaned_line:
                cleaned_lines.append(cleaned_line)
        
        cleaning_stats['file_stats'][file_name]['output'] = len(cleaned_lines)
        cleaning_stats['file_stats'][file_name]['invalid'] = len(lines) - len(cleaned_lines) - cleaning_stats['file_stats'][file_name].get('duplicates', 0)
        
        # 写入清洗后的文件
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write('\n'.join(cleaned_lines))
        
        return True
    except Exception as e:
        print(f"处理文件 {input_file} 时出错: {str(e)}")
        return False

# 处理gzip压缩文件
def process_gzip_file(input_file, output_file):
    global cleaning_stats
    
    file_name = os.path.basename(input_file)
    cleaning_stats['total_files'] += 1
    
    try:
        with gzip.open(input_file, 'rt', encoding='utf-8', errors='ignore') as f:
            lines = f.readlines()
        
        cleaning_stats['total_records'] += len(lines)
        cleaning_stats['file_stats'][file_name]['input'] = len(lines)
        
        cleaned_lines = []
        for line in lines:
            cleaned_line = process_log_line(line)
            if cleaned_line:
                cleaned_lines.append(cleaned_line)
        
        cleaning_stats['file_stats'][file_name]['output'] = len(cleaned_lines)
        cleaning_stats['file_stats'][file_name]['invalid'] = len(lines) - len(cleaned_lines) - cleaning_stats['file_stats'][file_name].get('duplicates', 0)
        
        # 写入清洗后的文件
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write('\n'.join(cleaned_lines))
        
        return True
    except Exception as e:
        print(f"处理压缩文件 {input_file} 时出错: {str(e)}")
        return False

# 主函数
def main():
    print("开始执行日志清洗操作...")
    
    # 遍历logs目录
    for file_name in os.listdir(logs_dir):
        input_path = os.path.join(logs_dir, file_name)
        
        # 跳过目录
        if not os.path.isfile(input_path):
            continue
        
        # 跳过已清洗的文件
        if file_name.endswith('.cleaned'):
            continue
        
        # 生成输出文件名
        if file_name.endswith('.gz'):
            output_file_name = f"{file_name.replace('.gz', '')}.cleaned"
        else:
            output_file_name = f"{file_name}.cleaned"
        output_path = os.path.join(logs_dir, output_file_name)
        
        print(f"处理文件: {file_name}")
        
        # 根据文件类型选择处理方法
        if file_name.endswith('.gz'):
            process_gzip_file(input_path, output_path)
        else:
            process_file(input_path, output_path)
    
    # 生成清洗报告
    print("\n=== 日志清洗报告 ===")
    print(f"总文件数: {cleaning_stats['total_files']}")
    print(f"总记录数: {cleaning_stats['total_records']}")
    print(f"处理后记录数: {cleaning_stats['processed_records']}")
    print(f"无效记录数: {cleaning_stats['invalid_records']}")
    print(f"重复记录数: {cleaning_stats['duplicate_records']}")
    print(f"清洗率: {(cleaning_stats['processed_records'] / cleaning_stats['total_records'] * 100):.2f}%")
    
    print("\n=== 按文件统计 ===")
    for file_name, stats in cleaning_stats['file_stats'].items():
        print(f"文件: {file_name}")
        print(f"  输入记录: {stats['input']}")
        print(f"  输出记录: {stats['output']}")
        print(f"  无效记录: {stats['invalid']}")
        print(f"  保留率: {(stats['output'] / stats['input'] * 100):.2f}%")
        print()
    
    print("日志清洗操作完成！")

if __name__ == "__main__":
    main()