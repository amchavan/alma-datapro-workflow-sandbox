from setuptools import setup

dependencies = [
    "draws-mock-icd",
    "drawsmb"
]

setup(name='draws-mock-pldriver',
      version='0.1',
      description='DRAWS Mock Pipeline Driver',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['draws', 'draws/mock', 'draws/mock/pldriver', 'draws/mock/pldriver/tasks'],
      install_requires=dependencies,
      zip_safe=False)
