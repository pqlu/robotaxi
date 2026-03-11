import re
from typing import Dict, List, Set
import pandas as pd


class AttributeDistributions:
    """Loads and combines TTS (Toronto Travel Survey) cross-tabulation matrices
    by demographic group into distributions indexed by origin ward."""

    HEADER_LINES_TO_SKIP = 22
    TABLE_PREFIX = 'TABLE'

    def get_distributions(self, path: str, groups: range) -> Dict[int, pd.DataFrame]:
        return self._group_distributions(path, groups)

    def _group_distributions(self, base_path: str, groups: range) -> Dict[int, pd.DataFrame]:
        matrices_by_group: Dict[int, Dict[int, pd.DataFrame]] = {}

        for group in groups:
            filepath = '{}/group{}.txt'.format(base_path, group)
            matrices_by_group[group] = self._load_matrices(filepath)

        origin_wards = range(1, 45)
        distributions: Dict[int, pd.DataFrame] = {}

        for ward in origin_wards:
            ward_matrices = []
            for group in matrices_by_group:
                if ward in matrices_by_group[group]:
                    ward_matrices.append(matrices_by_group[group][ward])
                else:
                    ward_matrices.append(pd.DataFrame())
            distributions[ward] = self._create_distribution(ward_matrices)

        return distributions

    def _load_matrices(self, filepath: str) -> Dict[int, pd.DataFrame]:
        with open(filepath, 'r') as file:
            raw = file.readlines()
        lines = list(map(str.strip, raw[self.HEADER_LINES_TO_SKIP:]))
        num_lines = len(lines)

        matrices: Dict[int, pd.DataFrame] = {}
        i = 0
        while i < num_lines:
            if lines[i][:5] == self.TABLE_PREFIX:
                origin_ward = int(re.search(r"\d+", lines[i]).group(0))
                i += 2  # skip empty line

                header = lines[i].split()
                i += 1

                rows = []
                while i < num_lines and lines[i] != '':
                    rows.append(lines[i].split())
                    i += 1

                df = pd.DataFrame(rows, columns=header).set_index('start_time')
                df = df.astype('int32')
                df = df.pivot(columns='ward_dest', values='total')
                matrices[origin_ward] = df
            i += 1

        return matrices

    def _create_distribution(self, group_matrices: List[pd.DataFrame]) -> pd.DataFrame:
        all_rows: Set = set(idx for matrix in group_matrices for idx in matrix.index)
        all_columns: Set = set(col for matrix in group_matrices for col in matrix.columns)
        num_groups = len(group_matrices)

        # Impute missing columns with zeros
        for col in all_columns:
            for i in range(num_groups):
                if col not in group_matrices[i].columns:
                    group_matrices[i][col] = 0

        # Merge all group matrices
        result = group_matrices[0]
        for i in range(1, num_groups):
            result = result.merge(group_matrices[i],
                                  how='outer',
                                  left_index=True,
                                  right_index=True,
                                  suffixes=[None, "_" + str(i)])

        result.index = result.index.astype(int)
        result.sort_index(inplace=True)
        result.fillna(0, inplace=True)

        # Combine columns from each group into lists
        for col in all_columns:
            col_group = [col] + ['{}_{}'.format(col, i) for i in range(1, num_groups)]
            result[col] = result[col_group].values.tolist()

        return result.loc[:, all_columns]


if __name__ == "__main__":
    age_path = 'tts/age'
    age_groups = range(1, 9)
    income_path = 'tts/income'
    income_groups = range(1, 8)

    dists = AttributeDistributions()

    print("Getting age distributions")
    age = dists.get_distributions(age_path, age_groups)
    print(age[1].head())

    print("Getting income distributions")
    income = dists.get_distributions(income_path, income_groups)
    print(income[1].head())
